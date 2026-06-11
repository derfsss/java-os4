/*
 * AWT Toolkit for AmigaOS 4 (Phase 4 M4) -- cacio-style: only top-level
 * windows get native peers (Intuition windows + a Java2D framebuffer);
 * everything inside is Swing (lightweight), so no native widgets exist.
 * AWT heavyweight widgets (Button, List, ...) are unsupported by design.
 *
 * Run with:
 *   -Dawt.toolkit=sun.awt.amiga.AmigaToolkit
 *   -Djava.awt.graphicsenv=sun.awt.amiga.AmigaGraphicsEnvironment
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.AWTException;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Insets;
import java.awt.JobAttributes;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PageAttributes;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.PrintJob;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.SystemTray;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.im.InputMethodHighlight;
import java.awt.image.ColorModel;
import java.awt.peer.ButtonPeer;
import java.awt.peer.CanvasPeer;
import java.awt.peer.CheckboxMenuItemPeer;
import java.awt.peer.CheckboxPeer;
import java.awt.peer.ChoicePeer;
import java.awt.peer.DesktopPeer;
import java.awt.peer.DialogPeer;
import java.awt.peer.FileDialogPeer;
import java.awt.peer.FontPeer;
import java.awt.peer.FramePeer;
import java.awt.peer.KeyboardFocusManagerPeer;
import java.awt.peer.LabelPeer;
import java.awt.peer.ListPeer;
import java.awt.peer.MenuBarPeer;
import java.awt.peer.MenuItemPeer;
import java.awt.peer.MenuPeer;
import java.awt.peer.MouseInfoPeer;
import java.awt.peer.PanelPeer;
import java.awt.peer.PopupMenuPeer;
import java.awt.peer.RobotPeer;
import java.awt.peer.ScrollPanePeer;
import java.awt.peer.ScrollbarPeer;
import java.awt.peer.SystemTrayPeer;
import java.awt.peer.TextAreaPeer;
import java.awt.peer.TextFieldPeer;
import java.awt.peer.TrayIconPeer;
import java.awt.peer.WindowPeer;
import java.util.Map;
import java.util.Properties;
import sun.awt.LightweightFrame;
import sun.awt.SunToolkit;
import sun.font.FontDesignMetrics;

public final class AmigaToolkit extends SunToolkit {

    public AmigaToolkit() {
        super();
        /* Swing double buffering: plain images, not VolatileImage churn */
        if (System.getProperty("swing.volatileImageBufferEnabled") == null)
            System.setProperty("swing.volatileImageBufferEnabled", "false");
    }

    /* expose SunToolkit's protected-static peer-map hooks to the peers */
    static void peerCreated(Object target, Object peer) {
        targetCreatedPeer(target, peer);
    }

    static void peerDisposed(Object target, Object peer) {
        targetDisposedPeer(target, peer);
    }

    @Override
    public sun.awt.datatransfer.DataTransferer getDataTransferer() {
        return null;
    }

    @Override
    public java.awt.im.spi.InputMethodDescriptor
            getInputMethodAdapterDescriptor() throws AWTException {
        return null;
    }

    private static UnsupportedOperationException unsupported(String what) {
        return new UnsupportedOperationException(what
            + " is not supported on AmigaOS (Swing-only toolkit)");
    }

    /* ------------------------- top-level windows ------------------------- */

    @Override
    public WindowPeer createWindow(Window target) throws HeadlessException {
        return new AmigaWindowPeer(target);
    }

    @Override
    public FramePeer createFrame(Frame target) throws HeadlessException {
        return new AmigaFramePeer(target);
    }

    @Override
    public FramePeer createLightweightFrame(LightweightFrame target)
            throws HeadlessException {
        throw unsupported("LightweightFrame");
    }

    @Override
    public DialogPeer createDialog(Dialog target) throws HeadlessException {
        return new AmigaDialogPeer(target);
    }

    /* ------------------ AWT heavyweight widgets: Swing-only -------------- */

    @Override
    public ButtonPeer createButton(Button target) throws HeadlessException {
        throw unsupported("java.awt.Button");
    }

    @Override
    public TextFieldPeer createTextField(TextField target)
            throws HeadlessException {
        throw unsupported("java.awt.TextField");
    }

    @Override
    public LabelPeer createLabel(Label target) throws HeadlessException {
        throw unsupported("java.awt.Label");
    }

    @Override
    public ListPeer createList(java.awt.List target) throws HeadlessException {
        throw unsupported("java.awt.List");
    }

    @Override
    public CheckboxPeer createCheckbox(Checkbox target)
            throws HeadlessException {
        throw unsupported("java.awt.Checkbox");
    }

    @Override
    public ScrollbarPeer createScrollbar(Scrollbar target)
            throws HeadlessException {
        throw unsupported("java.awt.Scrollbar");
    }

    @Override
    public ScrollPanePeer createScrollPane(ScrollPane target)
            throws HeadlessException {
        throw unsupported("java.awt.ScrollPane");
    }

    @Override
    public TextAreaPeer createTextArea(TextArea target)
            throws HeadlessException {
        throw unsupported("java.awt.TextArea");
    }

    @Override
    public ChoicePeer createChoice(Choice target) throws HeadlessException {
        throw unsupported("java.awt.Choice");
    }

    @Override
    public CanvasPeer createCanvas(Canvas target) {
        throw unsupported("java.awt.Canvas");
    }

    @Override
    public PanelPeer createPanel(Panel target) {
        throw unsupported("java.awt.Panel");
    }

    @Override
    public FileDialogPeer createFileDialog(FileDialog target)
            throws HeadlessException {
        throw unsupported("java.awt.FileDialog");
    }

    @Override
    public MenuBarPeer createMenuBar(MenuBar target) throws HeadlessException {
        throw unsupported("java.awt.MenuBar");
    }

    @Override
    public MenuPeer createMenu(Menu target) throws HeadlessException {
        throw unsupported("java.awt.Menu");
    }

    @Override
    public PopupMenuPeer createPopupMenu(PopupMenu target)
            throws HeadlessException {
        throw unsupported("java.awt.PopupMenu");
    }

    @Override
    public MenuItemPeer createMenuItem(MenuItem target)
            throws HeadlessException {
        throw unsupported("java.awt.MenuItem");
    }

    @Override
    public CheckboxMenuItemPeer createCheckboxMenuItem(CheckboxMenuItem target)
            throws HeadlessException {
        throw unsupported("java.awt.CheckboxMenuItem");
    }

    /* -------------------------- system services -------------------------- */

    @Override
    public DragSourceContextPeer createDragSourceContextPeer(
            DragGestureEvent dge) throws InvalidDnDOperationException {
        throw new InvalidDnDOperationException(
            "drag and drop is not supported on AmigaOS");
    }

    @Override
    public TrayIconPeer createTrayIcon(TrayIcon target)
            throws HeadlessException, AWTException {
        throw unsupported("SystemTray");
    }

    @Override
    public SystemTrayPeer createSystemTray(SystemTray target) {
        throw unsupported("SystemTray");
    }

    @Override
    public boolean isTraySupported() {
        return false;
    }

    @Override
    protected DesktopPeer createDesktopPeer(java.awt.Desktop target)
            throws HeadlessException {
        throw unsupported("java.awt.Desktop");
    }

    @Override
    public RobotPeer createRobot(Robot target, GraphicsDevice screen)
            throws AWTException {
        throw new AWTException("Robot is not supported on AmigaOS");
    }

    @Override
    public KeyboardFocusManagerPeer getKeyboardFocusManagerPeer()
            throws HeadlessException {
        return AmigaKeyboardFocusManagerPeer.getInstance();
    }

    @Override
    public FontPeer getFontPeer(String name, int style) {
        return null;
    }

    /* ------------------------------ screen ------------------------------- */

    @Override
    protected int getScreenWidth() {
        return AmigaGraphicsConfig.getScreenBounds().width;
    }

    @Override
    protected int getScreenHeight() {
        return AmigaGraphicsConfig.getScreenBounds().height;
    }

    @Override
    public int getScreenResolution() throws HeadlessException {
        return 72;
    }

    @Override
    public ColorModel getColorModel() throws HeadlessException {
        return ColorModel.getRGBdefault();
    }

    @Override
    public Insets getScreenInsets(java.awt.GraphicsConfiguration gc) {
        /* leave the Workbench title bar usable */
        return new Insets(32, 0, 0, 0);
    }

    /* ------------------------------- misc -------------------------------- */

    @Override
    public void sync() {
    }

    @Override
    public void beep() {
    }

    @Override
    public Clipboard getSystemClipboard() throws HeadlessException {
        throw unsupported("the system clipboard");
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String jobtitle, Properties props) {
        return null;
    }

    @Override
    public PrintJob getPrintJob(Frame frame, String jobtitle,
                                JobAttributes jobAttributes,
                                PageAttributes pageAttributes) {
        return null;
    }

    @Override
    public boolean isModalityTypeSupported(Dialog.ModalityType modalityType) {
        /* Real modality: AWT's ModalEventFilter does the EDT-level blocking
           purely in Java (it consults Window.isModalBlocked, set by
           Dialog.modalShow regardless of the peer), so a modal dialog blocks
           its parent and pumps its own events correctly.  The peer's
           setModalBlocked additionally drops native input to blocked windows.
           (Toolkit modal exclusion -- whole-app -- is not supported.) */
        return modalityType == Dialog.ModalityType.MODELESS
            || modalityType == Dialog.ModalityType.DOCUMENT_MODAL
            || modalityType == Dialog.ModalityType.APPLICATION_MODAL;
    }

    @Override
    public boolean isModalExclusionTypeSupported(
            Dialog.ModalExclusionType modalExclusionType) {
        return modalExclusionType == Dialog.ModalExclusionType.NO_EXCLUDE;
    }

    @Override
    public Map<java.awt.font.TextAttribute, ?> mapInputMethodHighlight(
            InputMethodHighlight highlight) throws HeadlessException {
        return null;
    }

    @Override
    public void grab(Window w) {
    }

    @Override
    public void ungrab(Window w) {
    }

    @Override
    protected boolean syncNativeQueue(final long timeout) {
        return true;
    }

    @Override
    public boolean isDesktopSupported() {
        return false;
    }

    @Override
    protected MouseInfoPeer getMouseInfoPeer() {
        throw unsupported("MouseInfo");
    }

    /* deprecated Toolkit abstracts */
    @Override
    public String[] getFontList() {
        return new String[] { Font.DIALOG, Font.SANS_SERIF, Font.SERIF,
                              Font.MONOSPACED, Font.DIALOG_INPUT };
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return FontDesignMetrics.getMetrics(font);
    }
}
