package org.jrenner.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.TimeUtils;
import org.jrenner.fps.entity.Entity;
import org.jrenner.fps.net.NetManager;
import org.jrenner.fps.net.packages.ChatMessage;

public class HUD {
	Stage stage;
	Skin skin;
	Label label;
	Label chatLabel;
	float bw = 200f * View.screenSizeRatio();
	float bh = 160f * View.screenSizeRatio();
	public TextButton forward, back, right, left, shoot;
	private Table arrowsTable;
	private TextField chatField;

	private ShapeRenderer shapes = new ShapeRenderer();
	private Array<ChatMessage> chatMessages = new Array<>();

	public HUD() {
		skin = Assets.skin;

		stage = new Stage();
		Table table = new Table();
		stage.addActor(table);
		table.setFillParent(true);
		table.align(Align.left | Align.top);
		table.setSkin(skin);
		label = new Label("", skin);
		table.add(label).row();

		Table chatTable = new Table();
		chatTable.align(Align.left | Align.bottom);
		chatTable.setSize(View.width(), View.height() / 2f);
		stage.addActor(chatTable);

		chatLabel = new Label("", Assets.chatLabelStyle);
		chatLabel.setFillParent(true);
		chatLabel.setAlignment(Align.left | Align.bottom);
		chatTable.add(chatLabel);

		// button interface for mobile
		if (Main.isMobile()) {
			Table btnTable = new Table();
			table.add(btnTable).left().top();

			TextButton camBtn = new TextButton("Camera", skin);
			camBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Toggleable.toggle(Toggleable.FREE_CAMERA);
				}
			});
			btnTable.add(camBtn).size(bw, bh);

			TextButton controlBtn = new TextButton("Control", skin);
			controlBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Toggleable.toggle(Toggleable.CONTROL_PLAYER);
				}
			});
			btnTable.add(controlBtn).size(bw, bh);

			arrowsTable = new Table();
			arrowsTable.setFillParent(true);
			stage.addActor(arrowsTable);
			arrowsTable.align(Align.right | Align.bottom);

			float sz = bw;
			float delta = sz;
			float baseX = (View.width() - delta) - 10f;
			float baseY = 10f;
			float dX = 0f;
			float dY = 0f;

			forward = new TextButton("For", skin);
			forward.setSize(sz, sz);
			forward.setPosition(baseX - delta, baseY + delta);
			right = new TextButton("Right", skin);
			right.setSize(sz, sz);
			right.setPosition(baseX + dX, baseY + dY);
			dX -= delta;
			back = new TextButton("Back", skin);
			back.setSize(sz, sz);
			back.setPosition(baseX + dX, baseY + dY);
			dX -= delta;
			left = new TextButton("Left", skin);
			left.setSize(sz, sz);
			left.setPosition(baseX + dX, baseY + dY);
			dX -= delta * 2f;

			TextButton jumpBtn = new TextButton("Jump", skin);
			jumpBtn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (Main.inst.client.player != null) {
						Main.inst.client.player.entity.jump();
					}
				}
			});
			jumpBtn.setSize(sz, sz);
			jumpBtn.setPosition(baseX + dX, baseY + dY);

			dX -= delta * 2;
			shoot = new TextButton("Shoot", skin);
			shoot.setPosition(baseX + dX, baseY + dY);
			shoot.setSize(sz, sz);

			stage.addActor(forward);
			stage.addActor(right);
			stage.addActor(back);
			stage.addActor(left);
			stage.addActor(jumpBtn);
			stage.addActor(shoot);
		}

		SpriteDrawable crossDraw = new SpriteDrawable(Crosshair.create());
		Image crosshair = new Image(crossDraw);
		crosshair.setCenterPosition(View.width() / 2, View.height() / 2);
		stage.addActor(crosshair);

		createChatField();
	}

	private StringBuilder sb = new StringBuilder();

	/** output extra debug info to the in-game HUD */
	boolean debugHUD = true;

	public void update() {
		if (Main.frame % 15 != 0) return;
		Player player = Main.inst.client.player;
		sb.delete(0, sb.length);
		sb.append("FPS: ").append(Integer.toString(Gdx.graphics.getFramesPerSecond()));
		if (Main.serverType != null) {
			sb.append("\nServerType: ").append(Main.serverType);
		}
		if (Main.inst.client != null) {
			sb.append("\nClientType: ").append(Main.inst.client.getClass().getSimpleName());
		}
		if (!Main.isLocalServer()) {
			sb.append("\nServer Address: ").append(NetManager.host);
		}
		if (!Main.isLocalServer()) {
			sb.append("\nPing: ").append(Integer.toString(Main.getNetClient().ping));
		}
		String camera = Toggleable.freeCamera() ? "Free" : "Player";
		String control = Toggleable.controlPlayer() ? "Player" : "Camera";
		String mouse = Toggleable.mouseLook() ? "Yes" : "No";
		sb.append("\nCamera: ").append(camera);
		sb.append("\nUser Control: ").append(control);
		sb.append("\nMouse Look: ").append(mouse);
		if (player != null && debugHUD) {
			sb.append("\nVelocity: ").append(Tools.fmt(player.entity.getVelocity(), 1));
			sb.append("\nPosition: ").append(Tools.fmt(player.entity.getPosition(), 1));
			if (player.entity.interpolator != null) {
				sb.append("\nPosition Error: ").append(Tools.fmt(player.entity.interpolator.posError, 2));
			}
			sb.append("\nOn Ground: ").append(player.entity.onGround);
			sb.append("\nYaw: ").append(player.entity.getYaw());
			sb.append("\nPitch: ").append(player.entity.getPitch());
			sb.append("\n\nPhysics time: ").append(String.format("%4s", Main.physicsTime));
			sb.append("\nGround dist: ").append(String.format("%.4f", player.entity.distFromGround));
			sb.append("\nGround normal: ").append(Tools.fmt(Physics.inst.getFloorNormal(player.entity.getPosition())));
			//sb.append("\nGroundPiece visibility: ").append(View.visibleGroundPieces).append(" / ").append(View.totalGroundPieces);
			//sb.append("\nGroundPiece visibility: ").append(View.visibleGroundPieces).append(" / ").append(LevelBuilder.groundPieces.size);
			sb.append("\nEntity visibility: ").append(View.visibleEntities).append(" / ").append(Entity.list.size);
		}
		sb.append("\nPress T to chat\nPress R to respawn");
		label.setText(sb.toString());

		sb.delete(0, sb.length);
		showChatMessages(sb);
		chatLabel.setText(sb.toString());

		if (queueOpenChat) {
			showChatFieldForRealIMeanIt();
		}
	}

	public void draw() {
		update();
		stage.act();
		stage.draw();
		shapes.end();
	}

	public void addChatMessage(ChatMessage chatMsg) {
		System.out.println("add chat message: " + chatMsg);
		chatMessages.add(chatMsg);
	}

	private static final long chatMessageLifeTime = 5000; // ms

	public void showChatMessages(StringBuilder sb) {
		for (ChatMessage chat : chatMessages) {
			long elapsed = TimeUtils.timeSinceMillis(chat.createTime);
			if (elapsed >= chatMessageLifeTime) {
				chatMessages.removeValue(chat, true);
			} else {
				sb.append("\n[");
				if (chat.playerId == -1) {
					sb.append("Server");
				} else {
					sb.append("Player ").append(chat.playerId);
				}
				sb.append("]: ").append(chat.text);
			}
		}
	}

	public boolean isChatOpen() {
		return stage.getKeyboardFocus() == chatField;
	}

	boolean queueOpenChat = false;

	// must be queued to stop the 't' from being entered into the field. Stupid. find a better way
	public void showChatField() {
		queueOpenChat = true;
	}

	private void showChatFieldForRealIMeanIt() {
		chatField.setVisible(true);
		stage.setKeyboardFocus(chatField);
		queueOpenChat = false;
	}

	public void hideChatField() {
		chatField.setVisible(false);
		stage.unfocus(chatField);
		clearChatField();
	}

	public void clearChatField() {
		chatField.setText("");
	}

	private void createChatField() {
		TextField.TextFieldStyle chatTFStyle = new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class));
		chatTFStyle.background = skin.getDrawable("button-up");

		chatField = new TextField("", chatTFStyle);
		chatField.setMaxLength(256);
		chatField.setPosition(0f, chatField.getHeight());
		stage.addActor(chatField);
		hideChatField();
		final TextField cf = chatField;
		chatField.setTextFieldListener(new TextField.TextFieldListener() {
			@Override
			public void keyTyped(TextField textField, char c) {
				if (c == '\r' || c == '\n') {
					ChatMessage chatMsg = new ChatMessage();
					chatMsg.text = cf.getText();
					Main.inst.client.sendChatMessage(chatMsg);
					clearChatField();
					hideChatField();
				}
			}
		});
		chatField.setTextFieldFilter(new TextField.TextFieldFilter() {
			@Override
			public boolean acceptChar(TextField textField, char c) {
				if (isChatOpen()) {
					return true;
				}
				return false;
			}
		});
	}

	public void getMoveTranslation(Vector3 trans) {
		trans.setZero();
		float x = 0f, y = 0f, z = 0f;
		if (forward.isPressed()) {
			z += 1f;
		}
		if (back.isPressed()) {
			z -= 1f;
		}
		if (left.isPressed()) {
			x += 1f;
		}
		if (right.isPressed()) {
			x -= 1f;
		}
		trans.set(x, y, z);
	}
}
