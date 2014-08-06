package org.jrenner.fps.input;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import org.jrenner.fps.Direction;
import org.jrenner.fps.Log;
import org.jrenner.fps.Main;
import org.jrenner.fps.Player;
import org.jrenner.fps.net.packages.CommandPackage;
import org.jrenner.fps.utils.Pooler;
import org.jrenner.fps.Toggleable;
import org.jrenner.fps.View;

import static com.badlogic.gdx.Input.Keys.*;

public class Input extends InputAdapter {
	private boolean[] pressed = new boolean[256];
	public ObjectMap<InputBind, Direction.Translation> transBindingMap = new ObjectMap<>();
	public ObjectMap<InputBind, Direction.Rotation> rotBindingMap = new ObjectMap<>();

	public Input() {
		InputBind.setDefaultBindings();
		transBindingMap.put(InputBind.MoveForward, Direction.Translation.Forward);
		transBindingMap.put(InputBind.MoveBack, Direction.Translation.Back);
		transBindingMap.put(InputBind.MoveLeft, Direction.Translation.Left);
		transBindingMap.put(InputBind.MoveRight, Direction.Translation.Right);
		transBindingMap.put(InputBind.MoveUp, Direction.Translation.Up);
		transBindingMap.put(InputBind.MoveDown, Direction.Translation.Down);

		rotBindingMap.put(InputBind.YawLeft, Direction.Rotation.YawLeft);
		rotBindingMap.put(InputBind.YawRight, Direction.Rotation.YawRight);
		rotBindingMap.put(InputBind.PitchUp, Direction.Rotation.PitchUp);
		rotBindingMap.put(InputBind.PitchDown, Direction.Rotation.PitchDown);
	}

	public void process() {
		if (Toggleable.controlPlayer()) {
			// TODO update interval
			playerControls();
		} else {
			cameraControls();
		}
		constrainMouse();
	}

	private void constrainMouse() {
		int x = Gdx.input.getX();
		int y = Gdx.input.getY();
		if (x < 0 || x > View.width || y < 0 || y > View.height) {
			lastScreenX = -1;
			lastScreenY = -1;
			Gdx.input.setCursorPosition(View.width / 2, View.height / 2);
		}

	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		//DynamicEntity.player.startShoot();
		InputBind ib = InputBind.matchByMouse(button);

		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		//DynamicEntity.player.stopShoot();
		return true;
	}

	@Override
	public boolean keyDown(int keycode) {
		if (View.inst.hud.isChatOpen()) {
			openedChatKeyDown(keycode);
			return true;
		}
		pressed[keycode] = true;
		switch (keycode) {
			case ESCAPE:
				Toggleable.toggle(Toggleable.MOUSE_LOOK);
				return true;
			case T:
				View.inst.hud.showChatField();
				return true;
			case P:
				Main.pause = !Main.pause;
				return true;
			case F1:
				Toggleable.toggle(Toggleable.DEBUG_DRAW);
				return true;
			case F2:
				Toggleable.toggle(Toggleable.PROFILE_GL);
				return true;
			case F4:
				Toggleable.toggle(Toggleable.CONTROL_PLAYER);
				return true;
			case C:
				Toggleable.toggle(Toggleable.FREE_CAMERA);
				return true;
		}
		return false;
	}

	private void openedChatKeyDown(int keycode) {
		if (keycode == ESCAPE) {
			View.inst.hud.hideChatField();
		}
	}

	protected static final float moveStep = 0.1f;
	protected static Vector3 tmp = new Vector3();
	protected static Vector3 tmp2 = new Vector3();

	@Override
	public boolean keyUp(int keycode) {
		pressed[keycode] = false;
		return false;
	}

	private int lastScreenX = -1;
	private int lastScreenY = -1;

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return mousePan(screenX, screenY);
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if (Main.isDesktop()) {
			return mousePan(screenX, screenY);
		}
		return false;
	}


	private boolean mousePan(int screenX, int screenY) {
		if (!Toggleable.mouseLook()) return false;
		if (lastScreenX < 0 || lastScreenY < 0) {
			lastScreenX = screenX;
			lastScreenY = screenY;
			return true;
		}
		float sensitivity = 0.2f;
		float yawDelta = 0.5f;
		float pitchDelta = 0.5f;
		float yaw = (screenX - lastScreenX) * yawDelta;
		float pitch = (screenY - lastScreenY) * pitchDelta;
		if (!Toggleable.controlPlayer()) {
			if (yaw != 0) {
				View.inst.rotateCamera(Direction.Rotation.YawRight, yaw * sensitivity);
			}
			if (pitch != 0) {
				View.inst.rotateCamera(Direction.Rotation.YawLeft, -pitch * sensitivity);
			}
		} else {
			Player player = Main.inst.client.player;
			if (player != null) {
				if (yaw != 0) {
					player.entity.adjustYaw(-yaw);
				}
				if (pitch != 0) {
					player.entity.adjustPitch(pitch);
				}
			}
		}
		lastScreenX = screenX;
		lastScreenY = screenY;
		return true;
	}

	public boolean isPressed(int code) {
		if (code < 0) return false;
		return pressed[code];
	}

	public boolean isShiftPressed() { return isPressed(SHIFT_LEFT) || isPressed(SHIFT_RIGHT); }

	public boolean isCtrlPressed() { return isPressed(CONTROL_LEFT) || isPressed(CONTROL_RIGHT); }

	// camera controls
	private void cameraControls() {
		float rotStep = 1f;
		if (InputBind.YawLeft.isPressed()) {
			View.inst.rotateCamera(Direction.Rotation.YawLeft, rotStep);
		}
		if (InputBind.YawRight.isPressed()) {
			View.inst.rotateCamera(Direction.Rotation.YawRight, rotStep);
		}
		if (InputBind.PitchUp.isPressed()) {
			View.inst.rotateCamera(Direction.Rotation.PitchUp, rotStep);
		}
		if (InputBind.PitchDown.isPressed()) {
			View.inst.rotateCamera(Direction.Rotation.PitchDown, rotStep);
		}
	}

	/** process player controls and send to server */
	public void playerControls() {
		Player player = Main.inst.client.player;
		if (player == null) return;
		CommandPackage cmdPack = Pooler.movementPackage();
		tmp.setZero();
		for (InputBind inputBind : transBindingMap.keys()) {
			if (inputBind.isPressed()) {
				Direction.Translation trans = transBindingMap.get(inputBind);
				cmdPack.setTranslation(trans);
				tmp.add(trans.vector);
			}
		}
		if (!tmp.isZero()) {
			player.entity.setRelativeDestinationByYaw(tmp.nor());
		} else {
			player.entity.setDestination(null);
		}

		for (InputBind inputBind : rotBindingMap.keys()) {
			if (inputBind.isPressed()) {
				player.entity.adjustRotation(rotBindingMap.get(inputBind));
			}
		}

		if (InputBind.Jump.isPressed()) {
			cmdPack.set(CommandPackage.JUMP, true);
			player.entity.jump();
		}

		boolean shooting = false;
		if (InputBind.Shoot.isPressed()) {
			cmdPack.set(CommandPackage.SHOOT, true);
			shooting = true;
		}
		if (Main.isServer()) {
			if (shooting) {
				player.entity.startShoot();
			} else {
				player.entity.stopShoot();
			}
		}

		if (isPressed(com.badlogic.gdx.Input.Keys.R)) {
			Main.inst.client.requestResetPosition();
		}

		cmdPack.setRotation(player.entity.getRotation());
		if (!Main.isServer()) {
			Main.getNetClient().sendClientUpdateToServer(cmdPack);
		}
		Pooler.free(cmdPack);
	}
}
