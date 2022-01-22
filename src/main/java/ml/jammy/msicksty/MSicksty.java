package ml.jammy.msicksty;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class MSicksty implements ClipboardOwner {
    private static boolean busy = false;
    private static MSicksty INSTANCE = null;

    public static void main(final String[] args) {
        LogManager.getLogManager().reset();
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent event) {
                if (busy || event.getKeyCode() != 3639) {
                    if (INSTANCE != null && INSTANCE.frame.isFocused()) {
                        INSTANCE.handleKey(event.getKeyCode());
                    }
                    return;
                }
                // PrtSc is pressed
                final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                BufferedImage capture = null;
                try {
                    capture = new Robot().createScreenCapture(screenRect);
                } catch (AWTException e) {
                    e.printStackTrace();
                }
                INSTANCE = new MSicksty(capture);
            }
            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {}
            @Override
            public void nativeKeyTyped(NativeKeyEvent e) {}
        });

    }

    private final BufferedImage capture;
    private final JFrame frame = new JFrame();

    private boolean rectangleSet = false;
    private Point corner1 = null;
    private Point corner2 = null;

    private boolean editMode = false;

    private int mode = 0; // 0 for arrows, 1 for righthooks

    private Arrow cArrow = null; // current arrow
    private final List<Arrow> arrows = new LinkedList<>();
    private Point p1 = null;
    private Point p2 = null;
    private final List<Righthook> righthooks = new LinkedList<>();

    public MSicksty(final BufferedImage capture) {
        this.capture = capture;
        if (capture == null) {
            busy = false;
            return;
        }

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                exit();
            }
        });

        frame.setUndecorated(true);
        GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].setFullScreenWindow(frame);

        frame.setContentPane(new JComponent() {
            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(capture, 0, 0, null);
                int brightness = (int)(256 - 256 * 0.8);
                g.setColor(new Color(0, 0, 0, brightness));
                g.fillRect(0, 0, getWidth(), getHeight());

                if (corner1 != null && corner2 != null) {
                    final Point min = getMin();
                    final Point max = getMax();
                    if (min.x == max.x || min.y == max.y) {
                        return;
                    }
                    g.drawImage(getSelected(), min.x, min.y, null);
                }
            }
        });
        frame.setVisible(true);
        frame.requestFocus();

        // rectangle selection
        frame.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (cArrow != null) {
                    cArrow.x2 = e.getX() - getMin().x;
                    cArrow.y2 = e.getY() - getMin().y;
                    frame.getContentPane().repaint();
                    return;
                }
                if (p2 != null) {
                    p2.move(e.getX(), e.getY());
                    frame.getContentPane().repaint();
                    return;
                }
                if (corner2 != null) {
                    corner2.move(e.getX(), e.getY());
                    frame.getContentPane().repaint();
                }
            }
        });
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    corner1 = new Point(0, 0);
                    corner2 = new Point(capture.getWidth(), capture.getHeight());
                    rectangleSet = true;


                    if (SwingUtilities.isRightMouseButton(e)) {
                        editMode = true;
                        frame.getContentPane().repaint();
                    } else {
                        copyAndExit();
                    }
                }
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                if (rectangleSet) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        final Point max = getMax();
                        final Point min = getMin();
                        if (e.getX() <= max.getX() && e.getX() >= min.getX() &&
                                e.getY() <= max.getY() && e.getY() >= min.getY()) {
                            // point is inside the selected area
                            if (mode == 0) {
                                cArrow = new Arrow(e.getX() - getMin().x, e.getY() - getMin().y);
                            } else { // mode is 1
                                p1 = new Point(e.getX(), e.getY());
                                p2 = new Point(e.getX(), e.getY());
                            }
                            return;
                        }
                    }

                    corner1 = null;
                    arrows.clear();
                }

                if (corner1 == null) {
                    corner1 = new Point(e.getX(), e.getY());
                    corner2 = new Point(e.getX(), e.getY());

                    editMode = SwingUtilities.isRightMouseButton(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (cArrow != null) {
                    cArrow.x2 = e.getX() - getMin().x;
                    cArrow.y2 = e.getY() - getMin().y;
                    frame.getContentPane().repaint();
                    arrows.add(cArrow);
                    cArrow = null;
                    return;
                }

                if (p2 != null) {
                    p2.move(e.getX(), e.getY());

                    if (p1.x == p2.x || p1.y == p2.y) {
                        p1 = null;
                        p2 = null;
                        frame.getContentPane().repaint();
                    } else {
                        righthooks.add(createRighthook());
                        p1 = null;
                        p2 = null;
                    }
                    return;
                }

                if (corner2 == null) {
                    return;
                }

                corner2.move(e.getX(), e.getY());

                if (corner1.x == corner2.x || corner1.y == corner2.y) {
                    corner1 = null;
                    corner2 = null;
                    frame.getContentPane().repaint();
                } else {
                    rectangleSet = true;
                    if (!editMode) {
                        copyAndExit();
                    }
                }
            }
        });
    }

    private BufferedImage getSelected() {
        final Point min = getMin();
        final Point max = getMax();
        final BufferedImage selected = cloneImage(capture.getSubimage(
                min.x, min.y,
                max.x - min.x,
                max.y - min.y
        ));

        for (final Arrow arrow : arrows) {
            arrow.draw(selected.getGraphics());
        }
        if (this.cArrow != null) {
            cArrow.draw(selected.getGraphics());
        }

        for (final Righthook righthook : righthooks) {
            righthook.draw(selected.getGraphics());
        }
        if (p1 != null && p2 != null) {
            createRighthook().draw(selected.getGraphics());
        }

        return selected;
    }

    private Righthook createRighthook() {
        return new Righthook(Math.min(p1.x, p2.x) - getMin().x, Math.min(p1.y, p2.y) - getMin().y, Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y));
    }

    private void handleKey(final int code) {
        // 1 ESC
        // 19 R
        // 30 A
        // 31 S
        // 44 Z
        // 46 C
        if (code == 1) { // ESC
            exit();
        }
        if (code == 19) { // R
            this.mode = 1; // righthooks
        }
        if (code == 30) { // A
            this.mode = 0; // arrows
        }
        if (code == 31) { // S
            final JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose save location and file");

            int userSelection = fileChooser.showSaveDialog(frame);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                final File fileToSave = fileChooser.getSelectedFile();
                System.out.println("Save as file: " + fileToSave.getAbsolutePath());
                try {
                    ImageIO.write(getSelected(), "png", fileChooser.getSelectedFile());
                    exit();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (code == 44) { // Z
            if (this.mode == 0 && this.arrows.size() > 0) {
                this.arrows.remove(this.arrows.size() - 1);
            }
            if (this.mode == 1 && this.righthooks.size() > 0) {
                this.righthooks.remove(this.righthooks.size() - 1);
            }

            this.frame.getContentPane().repaint();
        }
        if (code == 46) { // C
            copy();
        }
    }

    private void copy() {
        final Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        final Point min = getMin();
        final Point max = getMax();
        if (min.x == max.x || min.y == max.y) {
            // there's nothing to copy, exit without copying
            return;
        }
        c.setContents(new TransferableImage(getSelected()), this);
    }

    private void copyAndExit() {
        copy();
        exit();
    }

    private void exit() {
        frame.setVisible(false);
        frame.dispose();
        busy = false;
        INSTANCE = null;
    }

    private Point getMax() {
        return new Point(Math.max(corner1.x, corner2.x), Math.max(corner1.y, corner2.y));
    }

    private Point getMin() {
        return new Point(Math.min(corner1.x, corner2.x), Math.min(corner1.y, corner2.y));
    }

    public static BufferedImage cloneImage(final BufferedImage img) {
        final BufferedImage i = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        i.setData(img.getData());
        return i;
    }

    @Override
    public void lostOwnership(Clipboard c, Transferable t) {}

    private static final class TransferableImage implements Transferable {
        private final Image i;

        private TransferableImage(Image i) {
            this.i = i;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.imageFlavor) && i != null) {
                return i;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            final DataFlavor[] flavors = new DataFlavor[1];
            flavors[0] = DataFlavor.imageFlavor;
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            final DataFlavor[] flavors = getTransferDataFlavors();
            for (DataFlavor dataFlavor : flavors) {
                if (flavor.equals(dataFlavor)) {
                    return true;
                }
            }

            return false;
        }

    }
}
