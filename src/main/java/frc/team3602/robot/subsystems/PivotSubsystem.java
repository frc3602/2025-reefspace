/*
 * Copyright (C) 2025 Team 3602 All rights reserved. This work is
 * licensed under the terms of the MIT license which can be found
 * in the root directory of this project.
 */

package frc.team3602.robot.subsystems;

import java.util.function.DoubleSupplier;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.math.controller.ArmFeedforward;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import frc.team3602.robot.Constants.PivotConstants;

public class PivotSubsystem extends SubsystemBase {
    public final TalonFX pivotMotor = new TalonFX(2);

    // Controls
    private final PIDController pivotController = new PIDController(PivotConstants.KD, PivotConstants.KI, PivotConstants.KD);
    private final ArmFeedforward pivotFeedforward = new ArmFeedforward(PivotConstants.KS, PivotConstants.KG, PivotConstants.KV, PivotConstants.KA);

    private double simPivotEncoder;
    private double pivotEncoder;

    private double simTotalEffort = 0.0;

    // Simulation
    public final SingleJointedArmSim pivotSim = new SingleJointedArmSim(DCMotor.getFalcon500(1), PivotConstants.gearing, SingleJointedArmSim.estimateMOI(PivotConstants.lengthMeters, PivotConstants.massKg), 0.2, -10000, 100000, true, 0);
    private DoubleSupplier elevatorVizLength;
    private final MechanismRoot2d pivotRoot;
    private final MechanismLigament2d pivotViz;

    private double angleDeg = 0.0;

    public PivotSubsystem(MechanismRoot2d pivotRoot, DoubleSupplier elevatorVizLength) {
        var talonFXConfigs = new TalonFXConfiguration();

        // set slot 0 gains
        var slot0Configs = talonFXConfigs.Slot0;
        slot0Configs.kS = 0.25; // Add 0.25 V output to overcome static friction
        slot0Configs.kV = 0.12; // A velocity target of 1 rps results in 0.12 V output
        slot0Configs.kA = 0.01; // An acceleration of 1 rps/s requires 0.01 V output
        slot0Configs.kP = 4.8; // A position error of 2.5 rotations results in 12 V output
        slot0Configs.kI = 0; // no output for integrated error
        slot0Configs.kD = 0.1; // A velocity error of 1 rps results in 0.1 V output
        
        // set Motion Magic settings
        var motionMagicConfigs = talonFXConfigs.MotionMagic;
        motionMagicConfigs.MotionMagicCruiseVelocity = 80; // Target cruise velocity of 80 rps
        motionMagicConfigs.MotionMagicAcceleration = 160; // Target acceleration of 160 rps/s (0.5 seconds)
        motionMagicConfigs.MotionMagicJerk = 1600; // Target jerk of 1600 rps/s/s (0.1 seconds)

        pivotMotor.getConfigurator().apply(talonFXConfigs);

        // Simulation
        this.pivotRoot = pivotRoot;
        this.pivotViz = this.pivotRoot.append(new MechanismLigament2d("Pivot Ligament", 0.4, 90, 10.0, new Color8Bit(Color.kAliceBlue)));
        this.elevatorVizLength = elevatorVizLength;
    }

    private final MotionMagicVoltage m_request = new MotionMagicVoltage(0);
    public Command testMotionMagic (double newAngle){
        return run(() -> {
            pivotMotor.setControl(m_request.withPosition(newAngle));
        });
    }

    //  public Command testPivot(double voltage){
    //     return runEnd(() -> {
    //         pivotMotor.setVoltage(voltage);
    //     }, () -> {
    //         pivotMotor.setVoltage(0);
    // });
    // }

    public Command setAngle(double newAngleDeg) {
        return runOnce(() -> {
            angleDeg = newAngleDeg;
        });
    }

    public double simGetEffort() {
        return simTotalEffort = ((pivotFeedforward.calculate(simPivotEncoder, 0)) + (pivotController.calculate(Units.radiansToDegrees(simPivotEncoder), angleDeg)));
      }

    public void periodic() {
        SmartDashboard.putNumber("Pivot Motor Output", pivotMotor.getMotorVoltage().getValueAsDouble());

        // Update Sim
        pivotSim.setInput(pivotMotor.getMotorVoltage().getValueAsDouble());
        pivotSim.update(TimedRobot.kDefaultPeriod);
        pivotViz.setAngle(Math.toDegrees(pivotSim.getAngleRads()) /*+ (pivotMotor.getMotorVoltage().getValueAsDouble() * 0.02)*/); // pivot doesn't work //TODO set up like 2024 crescendo
        pivotRoot.setPosition(0.75, (0.1 + elevatorVizLength.getAsDouble()));
    }
}