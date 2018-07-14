package com.ermacaz.imagebackup.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ImageUpload {

    @SerializedName("doc_name")
    @Expose
    private String docName;
    @SerializedName("doc_contents")
    @Expose
    private String docContents;

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getDocContents() {
        return docContents;
    }

    public void setDocContents(String docContents) {
        this.docContents = docContents;
    }

    @Override
    public String toString() {
        return getDocName();
    }

}