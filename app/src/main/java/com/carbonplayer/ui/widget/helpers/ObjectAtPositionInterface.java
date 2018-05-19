package com.carbonplayer.ui.widget.helpers;

/**
 * Interface defining a function for getting an object for a specific position.
 * PagerAdapters need to implement this interface to work with WrapContentPagerAdapter
 */
public interface ObjectAtPositionInterface {

    /**
     * Returns the Object for the provided position, null if position doesn't match an object (i.e. out of bounds)
     **/
    Object getObjectAtPosition(int position);
}