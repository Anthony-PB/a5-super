package selector;

import static selector.SelectionModel.SelectionState.*;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import selector.SelectionModel.SelectionState;

/**
 * A graphical application for selecting and extracting regions of images.
 */
public class SelectorApp implements PropertyChangeListener {

    /**
     * Our application window.  Disposed when application exits.
     */
    private final JFrame frame;

    /**
     * Component for displaying the current image and selection tool.
     */
    private final ImagePanel imgPanel;

    /**
     * The current state of the selection tool.  Must always match the model used by `imgPanel`.
     */
    private SelectionModel model;

    /* Components whose state must be changed during the selection process. */
    private JMenuItem saveItem;
    private JMenuItem undoItem;
    private JButton cancelButton;
    private JButton undoButton;
    private JButton resetButton;
    private JButton finishButton;
    private final JLabel statusLabel;


    /**
     * Construct a new application instance.  Initializes GUI components, so must be invoked on the
     * Swing Event Dispatch Thread.  Does not show the application window (call `start()` to do
     * that).
     */
    public SelectorApp() {
        // Initialize application window
        frame = new JFrame("Selector");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add status bar
        statusLabel = new JLabel();
        frame.add(statusLabel, BorderLayout.PAGE_END);
        //  Add `statusLabel` to the bottom of our window.  Stylistic alteration of the
        //  label (i.e., custom fonts and colors) is allowed.
        //  See the BorderLayout tutorial [1] for example code that you can adapt.
        //  [1]: https://docs.oracle.com/javase/tutorial/uiswing/layout/border.html

        // Add image component with scrollbars
        imgPanel = new ImagePanel();
        //  Replace the following line with code to put scroll bars around `imgPanel` while
        //  otherwise keeping it in the center of our window.  The scroll pane should also be given
        //  a moderately large preferred size (e.g., between 400 and 700 pixels wide and tall).
        //  The Swing Tutorial has lots of info on scrolling [1], but for this task you only need
        //  the basics from lecture.
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/scrollpane.html
        JScrollPane scrollPane = new JScrollPane(imgPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Dimension scrollPanePreferredSize = new Dimension(600, 400);
        scrollPane.setPreferredSize(scrollPanePreferredSize);
        //JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        //JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
        //Dimension preferredSize = new Dimension(20, 40);
        //horizontalScrollBar.setPreferredSize(preferredSize);
        //verticalScrollBar.setPreferredSize(preferredSize);
        frame.add(scrollPane);
        // Add menu bar
        frame.setJMenuBar(makeMenuBar());

        // Add control buttons
        frame.add(makeControlPanel(), BorderLayout.LINE_END);
        // Controller: Set initial selection tool and update components to reflect its state
        setSelectionModel(new PointToPointSelectionModel(true));
        frame.pack(); // Pack components for layout
        frame.setVisible(true); // Make the frame visible
    }

    /**
     * Create and populate a menu bar with our application's menus and items and attach listeners.
     * Should only be called from constructor, as it initializes menu item fields.
     */
    private JMenuBar makeMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Create and populate File menu
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        saveItem = new JMenuItem("Save...");
        fileMenu.add(saveItem);
        JMenuItem closeItem = new JMenuItem("Close");
        fileMenu.add(closeItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);

        // Create and populate Edit menu
        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        undoItem = new JMenuItem("Undo");
        editMenu.add(undoItem);

        // Assign accelerators to menu items
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));

        // Controller: Attach menu item listeners
        openItem.addActionListener(e -> openImage());
        closeItem.addActionListener(e -> imgPanel.setImage(null));
        saveItem.addActionListener(e -> saveSelection());
        exitItem.addActionListener(e -> frame.dispose());
        undoItem.addActionListener(e -> model.undo());

        return menuBar;
    }

    /**
     * Return a panel containing buttons for controlling image selection.  Should only be called
     * from constructor, as it initializes button fields.
     */
    private JPanel makeControlPanel() {
        JPanel jPan = new JPanel(new GridLayout(0,2));
        cancelButton = new JButton("Cancel");
        undoButton = new JButton("Undo");
        resetButton = new JButton("Reset");
        finishButton = new JButton("Finish");
        jPan.add(cancelButton);
        jPan.add(undoButton);
        jPan.add(resetButton);
        jPan.add(finishButton);

        //Listeners
        cancelButton.addActionListener(e -> model.cancelProcessing());
        undoButton.addActionListener(e -> model.undo());
        resetButton.addActionListener(e -> model.reset());
        finishButton.addActionListener(e -> model.finishSelection());
        return jPan;
    }

    /**
     * Start the application by showing its window.
     */
    public void start() {
        // Compute ideal window size
        frame.pack();

        frame.setVisible(true);
    }

    /**
     * React to property changes in an observed model.  Supported properties include:
     * * "state": Update components to reflect the new selection state.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
                reflectSelectionState(model.state());
        }
    }

    /**
     * Update components to reflect a selection state of `state`.  Disable buttons and menu items
     * whose actions are invalid in that state, and update the status bar.
     */
    private void reflectSelectionState(SelectionState state) {
        // Update status bar to show current state
        statusLabel.setText(state.toString());
        // Disable buttons and menu items by default
        cancelButton.setEnabled(false);
        finishButton.setEnabled(false);
        saveItem.setEnabled(false);
        undoButton.setEnabled(false);
        resetButton.setEnabled(false);
        //Could have done an if else statement tree. Or something with ?. Wanted to try this.
        // Enable components based on the selection state
        switch (state) {
            case PROCESSING:
                cancelButton.setEnabled(true);
                break;
            case SELECTING:
                finishButton.setEnabled(true);
                undoButton.setEnabled(true);
                resetButton.setEnabled(true);
                break;
            case SELECTED:
                undoButton.setEnabled(true);
                resetButton.setEnabled(true);
                saveItem.setEnabled(true);
                break;
            case NO_SELECTION:
                // Nothing to enable for NO_SELECTION state
                break;
        }
    }

    /**
     * Return the model of the selection tool currently in use.
     */
    public SelectionModel getSelectionModel() {
        return model;
    }

    /**
     * Use `newModel` as the selection tool and update our view to reflect its state.  This
     * application will no longer respond to changes made to its previous selection model and will
     * instead respond to property changes from `newModel`.
     */
    public void setSelectionModel(SelectionModel newModel) {
        // Stop listening to old model
        if (model != null) {
            model.removePropertyChangeListener(this);
        }

        imgPanel.setSelectionModel(newModel);
        model = imgPanel.selection();
        model.addPropertyChangeListener("state", this);

        // Since the new model's initial state may be different from the old model's state, manually
        //  trigger an update to our state-dependent view.
        reflectSelectionState(model.state());
    }

    /**
     * Start displaying and selecting from `img` instead of any previous image.  Argument may be
     * null, in which case no image is displayed and the current selection is reset.
     */
    public void setImage(BufferedImage img) {
        imgPanel.setImage(img);
    }

    /**
     * Allow the user to choose a new image from an "open" dialog.  If they do, start displaying and
     * selecting from that image.  Show an error message dialog (and retain any previous image) if
     * the chosen image could not be opened.
     */
    private void openImage() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // Filter for file extensions supported by Java's ImageIO readers
        chooser.setFileFilter(new FileNameExtensionFilter("Image files",
                ImageIO.getReaderFileSuffixes()));
        int returnVal = 0;
        boolean issueAFO = false;
        while(!issueAFO) {
            returnVal = chooser.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) { // == 0
                try {
                    File selectedFile = chooser.getSelectedFile();
                    BufferedImage img = ImageIO.read(selectedFile);
                    if (img != null) {
                        this.setImage(img);
                        issueAFO = true; // allow the chooser to close
                    } else {
                        throw new IOException("Failed to read image.");
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                            "Error opening image: " + e.getMessage(),
                            "Unsupported Image Format",
                            JOptionPane.ERROR_MESSAGE);
                }
            }else{
                break;
            }
        }
        if (!issueAFO && returnVal != JFileChooser.CANCEL_OPTION) {
            openImage(); // Recursive call to show the dialog again
        }
        //  Complete this method as specified by performing the following tasks:
        //  * Show an "open file" dialog using the above chooser [1].
        //  * If the user selects a file, read it into a BufferedImage [2], then set that as the
        //    current image (by calling `this.setImage()`).
        //  * If a problem occurs when reading the file (either an exception is thrown or null is
        //    returned), show an error dialog with a descriptive title and message [3].
        //  [1] https://docs.oracle.com/javase/tutorial/uiswing/components/filechooser.html
        //  [2] https://docs.oracle.com/javase/tutorial/2d/images/loadimage.html
        //  [3] https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
        //  (embellishment): After a problem, re-show the open dialog.  By reusing the same
        //  chooser, the dialog will show the same directory as before the problem. (1 point)

    }

    /**
     * Save the selected region of the current image to a file selected from a "save" dialog.
     * Show an error message dialog if the image could not be saved.
     */
    private void saveSelection() {
        JFileChooser chooser = new JFileChooser();
        // Start browsing in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        // We always save in PNG format, so only show existing PNG files
        chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
        int returnVal = chooser.showSaveDialog(frame); // Use showSaveDialog for saving a file
        //File Output Stream
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                // Append .png extension if not already present
                // Embellishment (1)
                file = new File(file.getAbsolutePath() + ".png");
            }

            if (file.exists()) {
                // Prompt before overwriting existing file
                // Embellishment (2)
                int overwriteOption = JOptionPane.showConfirmDialog(frame,
                        "File already exists. Do you want to overwrite it?",
                        "Overwrite File", JOptionPane.YES_NO_CANCEL_OPTION);
                if (overwriteOption != JOptionPane.YES_OPTION) {
                    // (3)
                    saveSelection();
                    return; // User chose not to overwrite or canceled
                }
            }

            try {
                //ImageIO.write(dst, "png", file);
                FileOutputStream fileOut = new FileOutputStream(file);
                model.saveSelection(fileOut);
                JOptionPane.showMessageDialog(frame,
                        "Image saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame,
                        e.getMessage(),
                        e.getClass().getSimpleName(), JOptionPane.ERROR_MESSAGE);
                // (3)
                saveSelection();
            }
        }
    }

    /**
     * Run an instance of SelectorApp.  No program arguments are expected.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Set Swing theme to look the same (and less old) on all operating systems.
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                /* If the Nimbus theme isn't available, just use the platform default. */
            }
            // Create and start the app
            SelectorApp app = new SelectorApp();
            app.start();
        });
    }
}
