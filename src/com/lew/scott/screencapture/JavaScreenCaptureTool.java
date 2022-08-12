package com.lew.scott.screencapture;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * java编写截图工具 功能:用于截取图片,方便快捷!
 * 
 * @author zhonglijunyi@163.com
 * 
 */
public class JavaScreenCaptureTool extends Frame implements MouseListener, MouseMotionListener, KeyListener {

	private static final long serialVersionUID = 1L;
	private static final String title = "Java Screen Capture Tool";
	private int frameX, frameY, frameWidth, frameHeight;
	private int firstPointx, firstPointy;

	private BufferedImage bi;
	private Robot robot;

	private Rectangle rectangle;
	private Rectangle rectangleCursorUp, rectangleCursorDown, rectangleCursorLeft, rectangleCursorRight;
	private Rectangle rectangleCursorRU, rectangleCursorRD, rectangleCursorLU, rectangleCursorLD;
	private Image bis;
	private Point[] point = new Point[3];
	private int width, height;
	private int nPoints = 5;

	private boolean drawHasFinish = false, change = false;
	private int changeFirstPointX, changeFirstPointY, changeWidth, changeHeight;
	private boolean changeUP = false, changeDOWN = false, changeLEFT = false, changeRIGHT = false;
	private boolean changeRU = false, changeRD = false, changeLU = false, changeLD = false;
	private boolean redraw = false;

	// 菜单
	private JPopupMenu popup_;
	private JMenuItem saveItem, copyItem, clearItem, exitItem;
	private JMenu optionsMenu;
	JRadioButtonMenuItem doubleClickSave, doubleClickCopy;
	JRadioButtonMenuItem pressEnterSave, pressEnterCopy;
	JCheckBoxMenuItem exitAfterSave, exitAfterCopy;
	// options
	private boolean doubleClickSaveFlag = true, doubleClickCopyFlag;
	private boolean pressEnterSaveFlag, pressEnterCopyFlag = true;
	private boolean exitAfterSaveFlag = true, exitAfterCopyFlag = true;
	private static final int double_Click_Save_Code = 0x11;
	private static final int double_Click_Copy_Code = 0x12;
	private static final int press_Enter_Save_Code = 0x21;
	private static final int press_Enter_Copy_Code = 0x22;
	private static final int Exit_After_Save_Code = 0x01;
	private static final int Exit_After_Copy_Code = 0x02;
	private static final String CFG_FILE_NAME = "jsct.properties";

	//禁用缩放
	static {

		System.setProperty("sun.java2d.uiScale", "1");

	}

	private JavaScreenCaptureTool() {
		// 取得屏幕大小
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		if (gs.length == 1) {
			Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
			frameWidth = dimension.width;
			frameHeight = dimension.height;
		} else {// 多屏幕的情况
			// 主显示屏大小
			Rectangle defRect = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
			frameWidth = defRect.width;
			frameHeight = defRect.height;
			for (int i = 0; i < gs.length; i++) {
				GraphicsConfiguration gc = gs[i].getDefaultConfiguration();
				Rectangle bounds = gc.getBounds();
				frameX = (bounds.x < frameX) ? bounds.x : frameX;
				frameY = (bounds.y < frameY) ? bounds.y : frameY;
				// 主显示屏的bounds.x和bounds.y都为0
				if (bounds.x != 0) {
					frameWidth += bounds.width;
				}
				if (bounds.y != 0) {
					frameHeight += bounds.height;
				}
			}
			if (frameWidth <= 0 || frameHeight <= 0) {
				Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
				frameWidth = dimension.width;
				frameHeight = dimension.height;
			}
		}
		rectangle = new Rectangle(frameX, frameY, frameWidth, frameHeight);

		// 菜单
		popup_ = new JPopupMenu();
		saveItem = new JMenuItem("保存  (Ctrl+S)  ||  双击");
		saveItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		saveItem.addActionListener(new MyTakePicture());
		copyItem = new JMenuItem("复制  (Ctrl+C)");
		copyItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		copyItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				BufferedImage sbi;
				if (drawHasFinish) {
					sbi = bi.getSubimage(changeFirstPointX, changeFirstPointY, changeWidth, changeHeight);
				} else {
					sbi = bi;
				}
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				ImageTransferable selection = new ImageTransferable(sbi);
				clipboard.setContents(selection, null);
				if (exitAfterCopyFlag) {
					System.exit(0);
				}
			}
		});
		clearItem = new JMenuItem("重绘  (Ctrl+Z)");
		clearItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		clearItem.addActionListener(new MyClearPicture());
		optionsMenu = new JMenu("配置");
		ButtonGroup group = new ButtonGroup();
		doubleClickSave = new JRadioButtonMenuItem("双击保存");
		doubleClickSave.setSelected(doubleClickSaveFlag);
		doubleClickSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(double_Click_Save_Code);
				updateButtonText();
				updateCfgFile();
			}
		});
		doubleClickCopy = new JRadioButtonMenuItem("双击复制");
		doubleClickCopy.setSelected(doubleClickCopyFlag);
		doubleClickCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(double_Click_Copy_Code);
				updateButtonText();
				updateCfgFile();
			}
		});
		group.add(doubleClickSave);
		group.add(doubleClickCopy);
		ButtonGroup group2 = new ButtonGroup();
		pressEnterSave = new JRadioButtonMenuItem("回车保存");
		pressEnterSave.setSelected(pressEnterSaveFlag);
		pressEnterSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(press_Enter_Save_Code);
				updateCfgFile();
			}
		});
		pressEnterCopy = new JRadioButtonMenuItem("回车复制");
		pressEnterCopy.setSelected(pressEnterCopyFlag);
		pressEnterCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(press_Enter_Copy_Code);
				updateCfgFile();
			}
		});
		group2.add(pressEnterSave);
		group2.add(pressEnterCopy);
		exitAfterSave = new JCheckBoxMenuItem("保存后退出");
		exitAfterSave.setSelected(exitAfterSaveFlag);
		exitAfterSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(Exit_After_Save_Code);
				updateButtonText();
				updateCfgFile();
			}
		});
		exitAfterCopy = new JCheckBoxMenuItem("复制后退出");
		exitAfterCopy.setSelected(exitAfterCopyFlag);
		exitAfterCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setOptionAction(Exit_After_Copy_Code);
				updateButtonText();
				updateCfgFile();
			}
		});
		optionsMenu.add(doubleClickSave);
		optionsMenu.add(doubleClickCopy);
		optionsMenu.addSeparator();
		optionsMenu.add(pressEnterSave);
		optionsMenu.add(pressEnterCopy);
		optionsMenu.addSeparator();
		optionsMenu.add(exitAfterSave);
		optionsMenu.add(exitAfterCopy);
		exitItem = new JMenuItem("退出  (Esc)");
		exitItem.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		popup_.add(saveItem);
		popup_.add(copyItem);
		popup_.add(clearItem);
		popup_.add(optionsMenu);
		popup_.add(exitItem);
		// read configuration file
		readCfgFile();

		// robot
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}

		// 截取全屏
		bi = robot.createScreenCapture(rectangle);
		this.setTitle(title);
		this.setLocation(frameX, frameY);
		this.setSize(frameWidth, frameHeight);
		this.setUndecorated(true);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addKeyListener(this);
		this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
		this.setVisible(true);
		this.repaint();
	}

	// 设定配置信息
	private void setOptionAction(int action_code) {
		if ((action_code & 0x10) > 0) {
			doubleClickSaveFlag = (double_Click_Save_Code == action_code);
			doubleClickCopyFlag = (double_Click_Copy_Code == action_code);
		}
		if ((action_code & 0x20) > 0) {
			pressEnterSaveFlag = (press_Enter_Save_Code == action_code);
			pressEnterCopyFlag = (press_Enter_Copy_Code == action_code);
		}
		if (Exit_After_Save_Code == action_code) {
			exitAfterSaveFlag = !exitAfterSaveFlag;
		}
		if (Exit_After_Copy_Code == action_code) {
			exitAfterCopyFlag = !exitAfterCopyFlag;
		}
	}
	
	// 更新按钮文字
	private void updateButtonText(){
		if (doubleClickSaveFlag) {
			saveItem.setText("保存  (Ctrl+S)   ||   双击");
		} else {
			saveItem.setText("保存  (Ctrl+S)");
		}
		if (doubleClickCopyFlag) {
			copyItem.setText("复制  (Ctrl+C)   ||   双击");
		} else {
			copyItem.setText("复制  (Ctrl+C)");
		}

		if (exitAfterSaveFlag && exitAfterCopyFlag) {
			exitItem.setText("退出  (Esc)   ||   保存复制后");
		} else if (exitAfterSaveFlag) {
			exitItem.setText("退出  (Esc)   ||   保存后");
		} else if (exitAfterCopyFlag) {
			exitItem.setText("退出  (Esc)   ||   复制后");
		} else {
			exitItem.setText("退出  (Esc)");
		}
	}

	// read configuration file
	private void readCfgFile() {
		File cfgFile = new File(CFG_FILE_NAME);
		if (!cfgFile.exists()) {
			return;
		}
		Properties cfgProp = new Properties();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(cfgFile));
			String line = reader.readLine();
			while (null != line) {
				String[] prop = line.split("=");
				cfgProp.put(prop[0], prop[1]);
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if ("true".equals(cfgProp.get("" + double_Click_Save_Code))) {
			setOptionAction(double_Click_Save_Code);
			doubleClickSave.setSelected(true);
		}
		if ("true".equals(cfgProp.get("" + double_Click_Copy_Code))) {
			setOptionAction(double_Click_Copy_Code);
			doubleClickCopy.setSelected(true);
		}
		if ("true".equals(cfgProp.get("" + press_Enter_Save_Code))) {
			setOptionAction(press_Enter_Save_Code);
			pressEnterSave.setSelected(true);
		}
		if ("true".equals(cfgProp.get("" + press_Enter_Copy_Code))) {
			setOptionAction(press_Enter_Copy_Code);
			pressEnterCopy.setSelected(true);
		}
		// 一下两个配置，默认为true；
		if (!"true".equals(cfgProp.get("" + Exit_After_Save_Code))) {
			exitAfterSave.setSelected(false);
			exitAfterSaveFlag = false;
		}
		if (!"true".equals(cfgProp.get("" + Exit_After_Copy_Code))) {
			exitAfterCopy.setSelected(false);
			exitAfterCopyFlag = false;
		}
		// 更新按钮文字
		updateButtonText();
	}

	// write configuration file
	private void updateCfgFile() {
		File cfgFile = new File(CFG_FILE_NAME);
		try {
			PrintWriter writer = new PrintWriter(cfgFile);
			writer.println(double_Click_Save_Code + "=" + doubleClickSaveFlag);
			writer.println(double_Click_Copy_Code + "=" + doubleClickCopyFlag);
			writer.println(press_Enter_Save_Code + "=" + pressEnterSaveFlag);
			writer.println(press_Enter_Copy_Code + "=" + pressEnterCopyFlag);
			writer.println(Exit_After_Save_Code + "=" + exitAfterSaveFlag);
			writer.println(Exit_After_Copy_Code + "=" + exitAfterCopyFlag);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new JavaScreenCaptureTool();
	}

	public void paint(Graphics g) {
		this.drawR(g);
	}

	// 缓存图片
	public void update(Graphics g) {
		if (bis == null) {
			bis = this.createImage(frameWidth, frameHeight);
		}
		// System.out.println("update");
		Graphics ga = bis.getGraphics();
		Color c = ga.getColor();
		ga.setColor(Color.black);
		ga.fillRect(0, 0, frameWidth, frameHeight);
		ga.setColor(c);
		paint(ga);
		g.drawImage(bis, 0, 0, frameWidth, frameHeight, null);
	}

	// 绘制选择框
	public void drawR(Graphics g) {
		g.drawImage(bi, 0, 0, frameWidth, frameHeight, null);
		// System.out.println("drawR");

		if (point[1] != null && point[2] != null && !drawHasFinish && !redraw) {
			// System.out.println("drawR: main");
			int[] xPoints = { point[1].x, point[2].x, point[2].x, point[1].x, point[1].x };
			int[] yPoints = { point[1].y, point[1].y, point[2].y, point[2].y, point[1].y };
			width = (point[2].x - point[1].x) > 0 ? (point[2].x - point[1].x) : (point[1].x - point[2].x);
			height = (point[2].y - point[1].y) > 0 ? (point[2].y - point[1].y) : (point[1].y - point[2].y);
			changeWidth = width;
			changeHeight = height;

			g.setColor(Color.red);
			g.drawString(width + "*" + height, point[1].x, point[1].y - 5);
			// 画点
			/* int i; if() */
			if (point[1].x < point[2].x && point[1].y < point[2].y) {
				firstPointx = point[1].x;
				firstPointy = point[1].y;
			}
			if (point[1].x > point[2].x && point[1].y < point[2].y) {
				firstPointx = point[2].x;
				firstPointy = point[1].y;
			}
			if (point[1].x < point[2].x && point[1].y > point[2].y) {
				firstPointx = point[1].x;
				firstPointy = point[2].y;
			}
			if (point[1].x > point[2].x && point[1].y > point[2].y) {
				firstPointx = point[2].x;
				firstPointy = point[2].y;
			}

			g.fillRect(firstPointx - 2, firstPointy - 2, 5, 5);
			g.fillRect(firstPointx + (width) / 2, firstPointy - 2, 5, 5);
			g.fillRect(firstPointx + width - 2, firstPointy - 2, 5, 5);
			g.fillRect(firstPointx + width - 2, firstPointy + height / 2 - 2, 5, 5);
			g.fillRect(firstPointx + width - 2, firstPointy + height - 2, 5, 5);
			g.fillRect(firstPointx + (width) / 2, firstPointy + height - 2, 5, 5);
			g.fillRect(firstPointx - 2, firstPointy + height - 2, 5, 5);
			g.fillRect(firstPointx - 2, firstPointy + height / 2 - 2, 5, 5);
			// 画矩形
			// g.drawString("fafda", point[1].x-100, point[1].y-5);
			g.drawPolyline(xPoints, yPoints, nPoints);
		}

		if (change) {
			// System.out.println("drawR: change");
			g.setColor(Color.red);
			g.drawString(changeWidth + "*" + changeHeight, changeFirstPointX, changeFirstPointY - 5);

			g.fillRect(changeFirstPointX - 2, changeFirstPointY - 2, 5, 5);
			g.fillRect(changeFirstPointX + (changeWidth) / 2, changeFirstPointY - 2, 5, 5);
			g.fillRect(changeFirstPointX + changeWidth - 2, changeFirstPointY - 2, 5, 5);
			g.fillRect(changeFirstPointX + changeWidth - 2, changeFirstPointY + changeHeight / 2 - 2, 5, 5);
			g.fillRect(changeFirstPointX + changeWidth - 2, changeFirstPointY + changeHeight - 2, 5, 5);
			g.fillRect(changeFirstPointX + (changeWidth) / 2, changeFirstPointY + changeHeight - 2, 5, 5);
			g.fillRect(changeFirstPointX - 2, changeFirstPointY + changeHeight - 2, 5, 5);
			g.fillRect(changeFirstPointX - 2, changeFirstPointY + changeHeight / 2 - 2, 5, 5);

			g.drawRect(changeFirstPointX, changeFirstPointY, changeWidth, changeHeight);
		}
	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (doubleClickSaveFlag) {
				saveItem.doClick();
			} else if (doubleClickCopyFlag) {
				copyItem.doClick();
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		redraw = false;
	}

	public void mouseReleased(MouseEvent e) {
		// System.out.println("mouseReleased");
		if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
			if (!drawHasFinish) {
				saveItem.setToolTipText("Save Full Screen to File");
			} else {
				saveItem.setToolTipText("Save Select Area to File");
			}
			if (!drawHasFinish) {
				copyItem.setToolTipText("Copy Full Screen");
			} else {
				copyItem.setToolTipText("Copy Select Area");
			}
			popup_.show(this, e.getX(), e.getY());
			return;
		}
		if (!drawHasFinish && !redraw && point[1] != null && point[2] != null) {
			// System.out.println("mouseReleased: !drawHasFinish && !redraw");
			if (point[1].x < point[2].x && point[1].y < point[2].y) {
				firstPointx = point[1].x;
				firstPointy = point[1].y;
			}
			if (point[1].x > point[2].x && point[1].y < point[2].y) {
				firstPointx = point[2].x;
				firstPointy = point[1].y;
			}
			if (point[1].x < point[2].x && point[1].y > point[2].y) {
				firstPointx = point[1].x;
				firstPointy = point[2].y;
			}
			if (point[1].x > point[2].x && point[1].y > point[2].y) {
				firstPointx = point[2].x;
				firstPointy = point[2].y;
			}
			changeFirstPointX = firstPointx;
			changeFirstPointY = firstPointy;
			rectangleCursorUp = new Rectangle(firstPointx + 20, firstPointy - 10, width - 40, 20);
			rectangleCursorDown = new Rectangle(firstPointx + 20, firstPointy + height - 10, width - 40, 20);
			rectangleCursorLeft = new Rectangle(firstPointx - 10, firstPointy + 10, 20, height - 20);
			rectangleCursorRight = new Rectangle(firstPointx + width - 10, firstPointy + 10, 20, height - 20);
			rectangleCursorLU = new Rectangle(firstPointx - 10, firstPointy - 10, 30, 20);
			rectangleCursorLD = new Rectangle(firstPointx - 10, firstPointy + height - 10, 30, 20);
			rectangleCursorRU = new Rectangle(firstPointx + width - 10, firstPointy - 10, 20, 20);
			rectangleCursorRD = new Rectangle(firstPointx + width - 10, firstPointy + height - 10, 20, 20);
			drawHasFinish = true;
		}
		// 确定每边能改变大小的矩形
		if (drawHasFinish) {
			// System.out.println("mouseReleased: !drawHasFinish && !redraw");
			rectangleCursorUp = new Rectangle(changeFirstPointX + 20, changeFirstPointY - 10, changeWidth - 40, 20);
			rectangleCursorDown = new Rectangle(changeFirstPointX + 20, changeFirstPointY + changeHeight - 10,
					changeWidth - 40, 20);
			rectangleCursorLeft = new Rectangle(changeFirstPointX - 10, changeFirstPointY + 10, 20, changeHeight - 20);
			rectangleCursorRight = new Rectangle(changeFirstPointX + changeWidth - 10, changeFirstPointY + 10, 20,
					changeHeight - 20);
			rectangleCursorLU = new Rectangle(changeFirstPointX - 2, changeFirstPointY - 2, 10, 10);
			rectangleCursorLD = new Rectangle(changeFirstPointX - 2, changeFirstPointY + changeHeight - 2, 10, 10);
			rectangleCursorRU = new Rectangle(changeFirstPointX + changeWidth - 2, changeFirstPointY - 2, 10, 10);
			rectangleCursorRD = new Rectangle(changeFirstPointX + changeWidth - 2,
					changeFirstPointY + changeHeight - 2, 10, 10);
		}
	}

	// 改变选择框
	public void mouseDragged(MouseEvent e) {
		// System.out.println("mouseDragged");
		point[2] = e.getPoint();
		// if(!drawHasFinish){
		this.repaint();
		// }

		// 托动鼠标移动大小
		if (change) {
			// System.out.println("mouseDragged: change");
			if (changeUP) {
				changeHeight = changeHeight + changeFirstPointY - e.getPoint().y;
				changeFirstPointY = e.getPoint().y;
			}
			if (changeDOWN) {
				changeHeight = e.getPoint().y - changeFirstPointY;
			}
			if (changeLEFT) {
				changeWidth = changeWidth + changeFirstPointX - e.getPoint().x;
				changeFirstPointX = e.getPoint().x;
			}
			if (changeRIGHT) {
				changeWidth = e.getPoint().x - changeFirstPointX;
			}
			if (changeLU) {
				changeWidth = changeWidth + changeFirstPointX - e.getPoint().x;
				changeHeight = changeHeight + changeFirstPointY - e.getPoint().y;
				changeFirstPointX = e.getPoint().x;
				changeFirstPointY = e.getPoint().y;
			}
			if (changeLD) {
				changeWidth = changeWidth + changeFirstPointX - e.getPoint().x;
				changeHeight = e.getPoint().y - changeFirstPointY;
				changeFirstPointX = e.getPoint().x;
			}
			if (changeRU) {
				changeWidth = e.getPoint().x - changeFirstPointX;
				changeHeight = changeHeight + changeFirstPointY - e.getPoint().y;
				changeFirstPointY = e.getPoint().y;
			}
			if (changeRD) {
				changeWidth = e.getPoint().x - changeFirstPointX;
				changeHeight = e.getPoint().y - changeFirstPointY;
			}
			this.repaint();
		}
	}

	// 控制鼠标，判定是否在框线上
	public void mouseMoved(MouseEvent e) {
		// System.out.println("mouseMoved");
		point[1] = e.getPoint();
		// 改变鼠标的形状
		if (rectangleCursorUp != null && rectangleCursorUp.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));
			change = true;
			changeUP = true;
		} else if (rectangleCursorDown != null && rectangleCursorDown.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.S_RESIZE_CURSOR));
			change = true;
			changeDOWN = true;
		} else if (rectangleCursorLeft != null && rectangleCursorLeft.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
			change = true;
			changeLEFT = true;
		} else if (rectangleCursorRight != null && rectangleCursorRight.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
			change = true;
			changeRIGHT = true;
		} else if (rectangleCursorLU != null && rectangleCursorLU.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
			change = true;
			changeLU = true;
		} else if (rectangleCursorLD != null && rectangleCursorLD.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR));
			change = true;
			changeLD = true;
		} else if (rectangleCursorRU != null && rectangleCursorRU.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
			change = true;
			changeRU = true;
		} else if (rectangleCursorRD != null && rectangleCursorRD.contains(point[1])) {
			this.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
			change = true;
			changeRD = true;
		} else {
			this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
			changeUP = false;
			changeDOWN = false;
			changeRIGHT = false;
			changeLEFT = false;
			changeRU = false;
			changeRD = false;
			changeLU = false;
			changeLD = false;
		}
		if (change) {
			this.repaint();
		}
		// redraw = false;
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			exitItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_S && e.getModifiers() == KeyEvent.CTRL_MASK) {
			saveItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_C && e.getModifiers() == KeyEvent.CTRL_MASK) {
			copyItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_Z && e.getModifiers() == KeyEvent.CTRL_MASK) {
			clearItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if (pressEnterSaveFlag) {
				saveItem.doClick();
			} else if (pressEnterCopyFlag) {
				copyItem.doClick();
			}
		}
		if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			clearItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
			clearItem.doClick();
		}
		if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
			clearItem.doClick();
		}
	}

	private class MyClearPicture implements ActionListener {
		// 重绘
		public void actionPerformed(ActionEvent e) {
			// System.out.println("redraw");
			drawHasFinish = false;
			change = false;
			redraw = true;
			rectangleCursorUp = null;
			rectangleCursorDown = null;
			rectangleCursorLeft = null;
			rectangleCursorRight = null;
			rectangleCursorRU = null;
			rectangleCursorRD = null;
			rectangleCursorLU = null;
			rectangleCursorLD = null;
			changeWidth = 0;
			changeHeight = 0;
			repaint();
		}
	}

	private class MyTakePicture implements ActionListener {
		// 保存图片
		public void actionPerformed(ActionEvent e) {
			FileDialog fileDialog = new FileDialog(JavaScreenCaptureTool.this, "保存截图", FileDialog.SAVE);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					name = name.toLowerCase();
					if (name.endsWith(".jpeg") || name.endsWith(".jpg")) {
						return true;
					} else {
						return false;
					}
				}
			});
			fileDialog.setVisible(true);
			String dir = fileDialog.getDirectory();
			String fileName = fileDialog.getFile();
			// System.out.println(dir+"//"+fileName);
			if (dir != null && !"".equals(dir) && (fileName == null || "".equals(fileName))) {
				JOptionPane.showMessageDialog(JavaScreenCaptureTool.this, "Error file!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if ((dir == null || "".equals(dir)) && fileName != null && !"".equals(fileName)) {
				JOptionPane.showMessageDialog(JavaScreenCaptureTool.this, "Error file!", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (dir == null || fileName == null || "".equals(dir) || "".equals(fileName)) {
				return;
			}

			// 判断目录
			File dirFile = new File(dir);
			if (dirFile.exists()) {
				if (!dirFile.isDirectory()) {
					JOptionPane.showMessageDialog(JavaScreenCaptureTool.this, "\"" + dir + "\" is not a directory!",
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			} else {
				int ret = JOptionPane.showConfirmDialog(JavaScreenCaptureTool.this, "\"" + dir
						+ "\" is not exists, do you want to create this directory?", "Hint", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				if (ret == JOptionPane.YES_OPTION) {
					dirFile.mkdir();
				} else {
					return;
				}
			}

			// amend fileName
			String temp = fileName.toLowerCase();
			if (!temp.endsWith(".jpeg") && !temp.endsWith(".jpg")) {
				fileName = fileName + ".jpg";
			}

			if (changeWidth > 0) {
				BufferedImage sbi;
				if (drawHasFinish) {
					sbi = bi.getSubimage(changeFirstPointX, changeFirstPointY, changeWidth, changeHeight);
				} else {
					sbi = bi;
				}

				try {
					ImageIO.write(sbi, "jpeg", new File(dirFile, fileName));
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			if (exitAfterSaveFlag) {
				System.exit(0);
			}
		}
	}

	/**
	 * This class is a wrapper for the data transfer of image objects.
	 */
	private class ImageTransferable implements Transferable {
		/**
		 * Constructs the selection.
		 * 
		 * @param image
		 */
		public ImageTransferable(Image image) {
			theImage = image;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { DataFlavor.imageFlavor };
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(DataFlavor.imageFlavor);
		}

		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (flavor.equals(DataFlavor.imageFlavor)) {
				return theImage;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}

		private Image theImage;
	}
}
