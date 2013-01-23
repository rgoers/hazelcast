/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.nio.serialization;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class Data implements IdentifiedDataSerializable {

    public static final int ID = 0;
    public static final int NO_CLASS_ID = -1;

    public int type = -1;
    public ClassDefinition cd = null;
    public byte[] buffer = null;
    int partitionHash = -1;

    public Data() {
    }

    public Data(int type, byte[] bytes) {
        this.type = type;
        this.buffer = bytes;
    }

    public int size() {
        return (buffer == null) ? 0 : buffer.length;
    }

    public void postConstruct(SerializationContext context) {
        if (cd != null && cd instanceof ClassDefinitionBinaryProxy) {
            try {
                cd = ((ClassDefinitionBinaryProxy) cd).toReal(context);
            } catch (IOException e) {
                throw new HazelcastSerializationException(e);
            }
        }
    }

    public void writeData(ObjectDataOutput out) throws IOException {
        writeData((DataOutput) out);
    }

    public void readData(ObjectDataInput in) throws IOException {
        readData((DataInput) in);
    }

    public void readData(DataInput in) throws IOException {
        type = in.readInt();
        final int classId = in.readInt();
        if (classId != NO_CLASS_ID) {
            final int version = in.readInt();
            SerializationContext context = ((SerializationContextAware) in).getSerializationContext();
            cd = context.lookup(classId, version);
            int classDefSize = in.readInt();
            if (cd != null) {
                in.skipBytes(classDefSize);
            } else {
                byte[] classDefBytes = new byte[classDefSize];
                in.readFully(classDefBytes);
                cd = context.createClassDefinition(classDefBytes);
            }
        }
        int size = in.readInt();
        if (size > 0) {
            buffer = new byte[size];
            in.readFully(buffer);
        }
        partitionHash = in.readInt();
    }

    //Warning!!!!!!!
    //The following method : capacity() should be updated whenever writeData method is changes
    public void writeData(DataOutput out) throws IOException {
        out.writeInt(type);
        if (cd != null) {
            out.writeInt(cd.getClassId());
            out.writeInt(cd.getVersion());
            byte[] classDefBytes = cd.getBinary();
            out.writeInt(classDefBytes.length);
            out.write(classDefBytes);
        } else {
            out.writeInt(NO_CLASS_ID);
        }
        int size = size();
        out.writeInt(size);
        if (size > 0) {
            out.write(buffer);
        }
        out.writeInt(partitionHash);
    }

    //Caclucaltes the size of the binary after the Data is serialized.
    public int totalSize() {
        int total = 0;
        total += 4; //integer
        if (cd != null) {
            total += 4;
            total += 4;
            total += 4;
            total += cd.getBinary().length;
        } else {
            total += 4;
        }
        total += 4;
        if (size() > 0) {
            total += buffer.length;
        }
        total += 4;
        return total;
    }

    @Override
    public int hashCode() {
        if (buffer == null) return Integer.MIN_VALUE;
        // FNV (Fowler/Noll/Vo) Hash "1a"
        final int prime = 0x01000193;
        int hash = 0x811c9dc5;
        for (int i = buffer.length - 1; i >= 0; i--) {
            hash = (hash ^ buffer[i]) * prime;
        }
        return hash;
    }

    public int getPartitionHash() {
        if (partitionHash == -1) {
            if (buffer == null) {
                throw new IllegalStateException("Cannot hash null buffer");
            }
            partitionHash = hashCode();
        }
        return partitionHash;
    }

    public void setPartitionHash(int partitionHash) {
        this.partitionHash = partitionHash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Data))
            return false;
        if (this == obj)
            return true;
        Data data = (Data) obj;
        return type == data.type && size() == data.size()
                && equals(buffer, data.buffer);
    }

    // Same as Arrays.equals(byte[] a, byte[] a2) but loop order is reversed.
    private static boolean equals(final byte[] data1, final byte[] data2) {
        if (data1 == data2) {
            return true;
        }
        if (data1 == null || data2 == null) {
            return false;
        }
        final int length = data1.length;
        if (data2.length != length) {
            return false;
        }
        for (int i = length - 1; i >= 0; i--) {
            if (data1[i] != data2[i]) {
                return false;
            }
        }
        return true;
    }

    public int getId() {
        return ID;
    }

    public boolean isPortable() {
        return SerializationConstants.CONSTANT_TYPE_PORTABLE == type;
    }

    @Override
    public String toString() {
        return "Data{" +
                "type=" + type + ", " +
                "partitionHash=" + partitionHash +
                "} size= " + size();
    }
}