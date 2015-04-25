/*
 * Copyright (c) 1996, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.awt.datatransfer;


/**
 * This code was copied from the openjdk-7 sources: http://www.docjar.com/docs/api/sun/awt/datatransfer/package-index.html
 * All code not necessary for the sending of mails in this project has been removed.
 */
public class DataFlavor {

    public DataFlavor(Class<?> representationClass, String humanPresentableName) {
        String primaryType = "application";
        String subType = "x-java-serialized-object";
        if (representationClass == null) {
            throw new NullPointerException("representationClass");
        }
        mimeType = primaryType + "/" + subType;

        this.representationClass  = representationClass;
        if (representationClass == null) {
            throw new NullPointerException("representationClass");
        }
    }

    public DataFlavor(String mimeType, String humanPresentableName) {
        super();
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        try {
            initialize(mimeType, humanPresentableName, this.getClass().getClassLoader());
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("can't find specified class: " + cnfe.getMessage());
        } catch (Exception mtpe) {
            throw new IllegalArgumentException("failed to parse:" + mimeType);
        }
    }

    private void initialize(String mimeType, String humanPresentableName, ClassLoader classLoader) throws Exception {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }

        this.mimeType = mimeType;

        if ("application/x-java-serialized-object".equals(this.mimeType))
            throw new IllegalArgumentException("no representation class specified for:" + mimeType);
        else
            representationClass = java.io.InputStream.class; // default

    }

    public String getMimeType() {
        return (mimeType != null) ? mimeType.toString() : null;
    }

    public Class<?> getRepresentationClass() {
        return representationClass;
    }

    public boolean equals(Object o) {
        return ((o instanceof DataFlavor) && equals((DataFlavor)o));
    }

    public boolean equals(DataFlavor that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }

        if (representationClass == null) {
            if (that.getRepresentationClass() != null) {
                return false;
            }
        } else {
            if (!representationClass.equals(that.getRepresentationClass())) {
                return false;
            }
        }

        if (mimeType == null) {
            if (that.mimeType != null) {
                return false;
            }
        } else {
            if (!mimeType.equals(that.mimeType)) {
                return false;
            }
        }

        return true;
    }

    public int hashCode() {
        int total = 0;

        if (representationClass != null) {
            total += representationClass.hashCode();
        }

        if (mimeType != null) {
            total += mimeType.hashCode();
        }

        return total;
    }

    String mimeType;
    private Class       representationClass;

} // class DataFlavor
