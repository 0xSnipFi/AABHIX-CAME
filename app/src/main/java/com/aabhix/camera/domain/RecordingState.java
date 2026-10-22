package com.aabhix.camera.domain;

/** Saved is reached only after successful MediaStore publication and decoder validation. */
public enum RecordingState {
    IDLE, PREPARING, RECORDING, FINALIZING, SAVED, ERROR;
    public boolean busy(){return this==PREPARING||this==RECORDING||this==FINALIZING;}
}
