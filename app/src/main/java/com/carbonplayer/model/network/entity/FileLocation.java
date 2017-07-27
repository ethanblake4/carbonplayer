package com.carbonplayer.model.network.entity;

import com.carbonplayer.model.entity.enums.StorageType;

import java.io.File;

/**
 * Created by ethanelshyeb on 7/21/17.
 */

public class FileLocation {

    private StorageType storageType;
    private File fullPath;

    public FileLocation(StorageType storageType, File fullPath){
        this.storageType = storageType;
        this.fullPath = fullPath;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public File getFullPath() {
        return fullPath;
    }

    public void setFullPath(File fullPath) {
        this.fullPath = fullPath;
    }
}
