package org.firstinspires.ftc.teamcode.Hardware;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class Lift extends Subsystem
{
    private DcMotor intake, lift;
    private Servo gateIn, gateOut, slope;
    private States state;
    private Level level;
    private ElapsedTime runtime = new ElapsedTime();
    private double intakeSpeed = 0;
    private double liftPower = 0;

    private RevColorSensorV3 color;
    private TouchSensor limit;


    public Lift(HardwareMap map, Telemetry telemetry) {
        super(telemetry);
        state = States.INTAKE;
        level = Level.INTAKE;

        lift = map.get(DcMotor.class, "leftLift");
        intake = map.get(DcMotor.class, "intake");

        gateIn = map.get(Servo.class, "gateIn");
        gateOut = map.get(Servo.class, "gateOut");
        slope = map.get(Servo.class, "slope");

        slope.setPosition(0.2);

        lift.setDirection(DcMotorSimple.Direction.REVERSE);

        lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER );

        lift.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        color = map.get(RevColorSensorV3.class, "color");
        limit = map.get(TouchSensor.class, "Limit");

        telemetry.addData("Lift Status", "Initialized");
    }

    @Override
    public void updateState() {
        switch(state) {

            case INTAKE:
                if(color.alpha() >10000 ){
                    state = States.IN;
                    runtime.reset();
                }
                else{
                    intake.setPower(intakeSpeed);
                    gateOut.setPosition(0.7);
                    gateIn.setPosition(1);
                    slope.setPosition(0.6);
                }


                break;
            case IN:
                gateIn.setPosition(0.33);
                slope.setPosition(1);

                if(runtime.milliseconds() >1000){
                    intake.setPower(0);
                }
                else{
                    intake.setPower(-0.5);
                }
                break;

            case MOVE:
//
                lift.setPower(liftPower);
                if(setLiftPos(level.numTicks))
                {
                    if(level == Level.INTAKE){
                        state = States.INTAKE;
                    }
                    else{
                        state = States.ATLEVEL;
                    }


                }


                break;
            case ATLEVEL:
                runtime.reset();
                break;
            case DUMP:
                gateOut.setPosition(0.3);
                if(runtime.milliseconds()>1500){
                    level = Level.INTAKE;
                    state = States.MOVE;
                }
                break;
        }

    }

    @Override
    public void updateTeleopState(GamePadEx gp1, GamePadEx gp2)
    {
        switch(state) {
            case INTAKE:
                this.intakeSpeed = gp2.getAxis(GamePadEx.ControllerAxis.LEFT_Y);
                runtime.reset();
                if(gp1.getControlDown(GamePadEx.ControllerButton.B)){
                    state = States.IN;
                }
                break;

            case IN:
                if(gp2.getControlDown(GamePadEx.ControllerButton.B)){
                    state = States.MOVE;
                }
                break;
            case MOVE:
                break;

            case ATLEVEL:
                if(gp2.getControlDown(GamePadEx.ControllerButton.B)){
                    state = States.DUMP;
                }

                break;

            case DUMP:
                break;

        }
        if(gp2.getControlDown(GamePadEx.ControllerButton.X)) {
            level = Level.TOP;
        }
        else if(gp2.getControlDown(GamePadEx.ControllerButton.A)) {
            level = Level.BOTTOM;
        }
        else if(gp2.getControlDown(GamePadEx.ControllerButton.GUIDE)){
            state = States.MOVE;
            level = Level.INTAKE;
        }
    }

    @Override
    public void stop() {

    }

    public boolean setLiftPos(int numTicks)
    {
        lift.setTargetPosition(numTicks);

        lift.setMode(DcMotor.RunMode.RUN_TO_POSITION);


        if (level == Level.INTAKE || level == Level.BOTTOM) {
            if(limit.isPressed()){
                return true;
            }

        } else {
            if (Math.abs(lift.getCurrentPosition() - numTicks) <= 5) {
                lift.setPower(0.0);

                lift.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

                return true;
            }
        }

        return false;
    }

    enum States
    {
        INTAKE,
        MOVE,
        ATLEVEL,
        DUMP,
        IN,
    }

    enum Level
    {
        TOP(0),
        MIDDLE(0),
        BOTTOM(0),
        INTAKE(0);

        public final int numTicks;

        private Level(int ticks)
        {
            this.numTicks = ticks;
        }

    }
}
