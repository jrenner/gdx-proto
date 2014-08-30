package org.jrenner.fps.net.packages;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Pool;
import org.jrenner.fps.Direction;

/** CommandPackage is the input data from the client that is sent over the net to an online server */
public class CommandPackage implements Pool.Poolable {
	public static final int FORWARD      = 0x01;
	public static final int BACK         = 0x02;
	public static final int STRAFE_LEFT  = 0x04;
	public static final int STRAFE_RIGHT = 0x08;
	public static final int UP           = 0x10;
	public static final int DOWN         = 0x20;
	public static final int JUMP         = 0x40;
	public static final int SHOOT        = 0x80;

	public int commandBits;
	public float yaw;
	public float pitch;
	public float roll;

	public void setTranslation(Direction.Translation trans) {
		switch (trans) {
			case Forward:
				set(FORWARD, true);
				break;
			case Back:
				set(BACK, true);
				break;
			case Up:
				set(UP, true);
				break;
			case Down:
				set(DOWN, true);
				break;
			case Left:
				set(STRAFE_LEFT, true);
				break;
			case Right:
				set(STRAFE_RIGHT, true);
				break;
			default:
				throw new GdxRuntimeException("unhandled");
		}
	}

	public void setRotation(Quaternion q) {
		yaw = q.getYaw();
		pitch = q.getPitch();
		roll = q.getRoll();
	}

	/*public void setRotation(Direction.Rotation rot) {
		switch (rot) {
			case YawLeft:
				set(YAW_LEFT, true);
				break;
			case YawRight:
				set(YAW_RIGHT, true);
				break;
			case PitchUp:
				set(PITCH_UP, true);
				break;
			case PitchDown:
				set(PITCH_DOWN, true);
				break;
			case RollLeft:
				set(ROLL_LEFT, true);
				break;
			case RollRight:
				set(ROLL_RIGHT, true);
				break;
		}
	}*/

	public void set(int value, boolean status) {
		if (status) {
			commandBits |= value; // on
		} else {
			commandBits &= ~(value); // off
		}
	}

	public boolean get(int value) {
		return (value & commandBits) != 0;
	}

	public void set(CommandPackage other) {
		commandBits = other.commandBits;
	}

	@Override
	public void reset() {
		commandBits = 0;
	}
}
