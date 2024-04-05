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
        // Handle wrapping around if the starting point of the selection is moved
        if (index == 0) {
            PolyLine firstSegment = listIterator.next(); // Move to the first segment
            p1 = firstSegment.start();
            p2 = firstSegment.end();

            // Replace the first segment
            PolyLine updatedFirstSegment = new PolyLine(newPoint, p2);
            listIterator.set(updatedFirstSegment);

            // Move to the last segment
            while (listIterator.hasNext()) {
                listIterator.next();
            }
            // Access the last segment using previous()
            PolyLine lastSegment = listIterator.previous();
            p1 = lastSegment.start();

            // Update the start of the last segment
            PolyLine updatedLastSegment = new PolyLine(p1, newPoint);
            listIterator.set(updatedLastSegment);

            propSupport.firePropertyChange("selection", null, selection());
            return;
        }

        // Track the current segment index
        int track = 0;

        while (listIterator.hasNext()) {
            PolyLine segment = listIterator.next();
            if (track == index) {
                // Update the end of the previous segment
                if (listIterator.hasPrevious()) {
                    PolyLine previousSegment = listIterator.previous();
                    listIterator.previous();
                    p1 = previousSegment.start();
                    PolyLine updatedPreviousSegment = new PolyLine(p1, newPoint);
                    listIterator.set(updatedPreviousSegment);
                    listIterator.next(); // Move back to the current segment
                    listIterator.next();
                }

                // Update the start of the current segment
                p2 = segment.end();
                PolyLine updatedCurrentSegment = new PolyLine(newPoint, p2);
                listIterator.set(updatedCurrentSegment);

                propSupport.firePropertyChange("selection", null, selection());
                return;
            }

            track++;
        }
        // TODO 4B: Complete the implementation of this method as specified using a `ListIterator`.
        //  You will need to replace two segments of the selection with different PolyLines, and
        //  this replacement can be done efficiently while iterating by using `ListIterator`'s
        //  `set()` method.  Think carefully about how to "wrap around" if `index` corresponds to
        //  the starting point of the selection.
        //  Reminder: If the moved point corresponds to the starting point for the selection, then
        //  you will also need to update the `start` field appropriately while avoiding rep exposure
        //  (remember that `Point` is a mutable class, so you will want to _copy_ client-provided
        //  Points rather than aliasing them).
        //  Finally, notify listeners that the "selection" property has changed (see parent class
        //  for examples).
    }

}
