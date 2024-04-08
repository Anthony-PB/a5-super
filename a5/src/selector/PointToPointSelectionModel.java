package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {
        //  Test immediately with `testLiveWireEmpty()`, and think about how the test might change
        //  for non-empty selections (see task 2D).
        return new PolyLine(lastPoint(),p);
    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
        selection.add(new PolyLine(lastPoint(), p));
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }
        // Create a copy of the new position to avoid modifying the original
        Point newPoint = new Point(newPos);
        Point p1, p2;

        // Get a list iterator to traverse the selection segments
        ListIterator<PolyLine> listIterator = selection.listIterator();
        int track = 0;
        // while loop
        if (index == 0) {
            start = newPoint;
        }
        while(listIterator.hasNext()){
            PolyLine poly = listIterator.next();
            if(track == index) {
                p1 = poly.end();
                listIterator.set(new PolyLine(newPos, p1));
            }
            // Prevents use of previous() and handle wrapping if needed
            if(track+1 == index || (index == 0 && !(listIterator.hasNext()))){
                p2 = poly.start();
                listIterator.set(new PolyLine(p2, newPos));
            }
            track++;
        }
        propSupport.firePropertyChange("selection", null, selection());
    }
}
