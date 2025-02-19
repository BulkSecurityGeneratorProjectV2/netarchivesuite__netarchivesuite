/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.distribute;

import java.io.File;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Version of RemoteFile that reads/writes a file to local storage.
 * <p>
 * 
 * <pre>
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 2, 2005
 * Time: 3:09:26 PM
 * </pre>
 */
@SuppressWarnings({"serial"})
public class TestRemoteFile extends HTTPRemoteFile implements RemoteFile {
    public boolean failsOnCopy;

    public static Map<RemoteFile, String> remainingRemoteFiles = new WeakHashMap<RemoteFile, String>();

    public TestRemoteFile(File localFile, boolean useChecksum, boolean fileDeletable, boolean multipleDownloads)
            throws IOFailure {
        super(localFile, useChecksum, fileDeletable, multipleDownloads);
        remainingRemoteFiles.put(this, localFile.getName());
    }

    @Test
    @Ignore("Fix me")
    public static RemoteFile getInstance(File remoteFile, Boolean useChecksums, Boolean fileDeletable,
            Boolean multipleDownloads) throws IOFailure {
        return new TestRemoteFile(remoteFile, useChecksums, fileDeletable, multipleDownloads);
    }

    public void copyTo(File destFile) {
        if (failsOnCopy) {
            throw new IOFailure("Expected IO error in copying " + "- you told me so!");
        }
        super.copyTo(destFile);
    }

    public void appendTo(OutputStream out) {
        if (failsOnCopy) {
            throw new IOFailure("Expected IO error in copying " + "- you told me so!");
        }
        super.appendTo(out);
    }

    public void cleanup() {
        remainingRemoteFiles.remove(this);
        super.cleanup();
    }

    public boolean isDeleted() {
        return !remainingRemoteFiles.containsKey(this);
    }

    public String toString() {
        return "TestRemoteFile: '" + file.getPath() + "'";
    }

    /** Remove any remote files that may have been left over. */
    public static void removeRemainingFiles() {
        // Must copy keyset to avoid concurrent modificaion of set
        for (RemoteFile rf : new HashSet<RemoteFile>(remainingRemoteFiles.keySet())) {
            rf.cleanup();
        }
        remainingRemoteFiles.clear();
    }

    /**
     * Give the set of remaining remote files.
     *
     * @return the Set of remaining files
     */
    public static Set<RemoteFile> remainingFiles() {
        return remainingRemoteFiles.keySet();
    }

    public File getFile() {
        return file;
    }

    protected boolean isLocal() {
        return true;
    }
}
