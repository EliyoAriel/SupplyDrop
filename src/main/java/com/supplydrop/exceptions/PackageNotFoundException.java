package com.supplydrop.exceptions;

public class PackageNotFoundException extends Exception {

    private final String packageName;

    public PackageNotFoundException(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }
}
