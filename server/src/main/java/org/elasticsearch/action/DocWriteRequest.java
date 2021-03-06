/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.action;

import org.elasticsearch.Version;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.index.VersionType;

import java.io.IOException;
import java.util.Locale;

import static org.elasticsearch.action.ValidateActions.addValidationError;
import static org.elasticsearch.index.seqno.SequenceNumbers.UNASSIGNED_PRIMARY_TERM;
import static org.elasticsearch.index.seqno.SequenceNumbers.UNASSIGNED_SEQ_NO;

/**
 * Generic interface to group ActionRequest, which perform writes to a single document
 * Action requests implementing this can be part of {@link org.elasticsearch.action.bulk.BulkRequest}
 */
public interface DocWriteRequest<T> extends IndicesRequest {

    /**
     * Set the index for this request
     * @return the Request
     */
    T index(String index);

    /**
     * Get the index that this request operates on
     * @return the index
     */
    String index();


    /**
     * Set the type for this request
     * @return the Request
     */
    T type(String type);

    /**
     * Get the type that this request operates on
     * @return the type
     */
    String type();

    /**
     * Get the id of the document for this request
     * @return the id
     */
    String id();

    /**
     * Get the options for this request
     * @return the indices options
     */
    IndicesOptions indicesOptions();

    /**
     * Set the routing for this request
     * @return the Request
     */
    T routing(String routing);

    /**
     * Get the routing for this request
     * @return the Routing
     */
    String routing();


    /**
     * Get the parent for this request
     * @return the Parent
     */
    String parent();

    /**
     * Get the document version for this request
     * @return the document version
     */
    long version();

    /**
     * Sets the version, which will perform the operation only if a matching
     * version exists and no changes happened on the doc since then.
     */
    T version(long version);

    /**
     * Get the document version type for this request
     * @return the document version type
     */
    VersionType versionType();

    /**
     * Sets the versioning type. Defaults to {@link VersionType#INTERNAL}.
     */
    T versionType(VersionType versionType);

    /**
     * only perform this request if the document was last modification was assigned the given
     * sequence number. Must be used in combination with {@link #setIfPrimaryTerm(long)}
     *
     * If the document last modification was assigned a different sequence number a
     * {@link org.elasticsearch.index.engine.VersionConflictEngineException} will be thrown.
     */
    T setIfSeqNo(long seqNo);

    /**
     * only performs this request if the document was last modification was assigned the given
     * primary term. Must be used in combination with {@link #setIfSeqNo(long)}
     *
     * If the document last modification was assigned a different term a
     * {@link org.elasticsearch.index.engine.VersionConflictEngineException} will be thrown.
     */
    T setIfPrimaryTerm(long term);

    /**
     * If set, only perform this request if the document was last modification was assigned this sequence number.
     * If the document last modification was assigned a different sequence number a
     * {@link org.elasticsearch.index.engine.VersionConflictEngineException} will be thrown.
     */
     long ifSeqNo();

    /**
     * If set, only perform this request if the document was last modification was assigned this primary term.
     *
     * If the document last modification was assigned a different term a
     * {@link org.elasticsearch.index.engine.VersionConflictEngineException} will be thrown.
     */
    long ifPrimaryTerm();

    /**
     * Get the requested document operation type of the request
     * @return the operation type {@link OpType}
     */
    OpType opType();

    /**
     * Requested operation type to perform on the document
     */
    enum OpType {
        /**
         * Index the source. If there an existing document with the id, it will
         * be replaced.
         */
        INDEX(0),
        /**
         * Creates the resource. Simply adds it to the index, if there is an existing
         * document with the id, then it won't be removed.
         */
        CREATE(1),
        /** Updates a document */
        UPDATE(2),
        /** Deletes a document */
        DELETE(3);

        private final byte op;
        private final String lowercase;

        OpType(int op) {
            this.op = (byte) op;
            this.lowercase = this.toString().toLowerCase(Locale.ROOT);
        }

        public byte getId() {
            return op;
        }

        public String getLowercase() {
            return lowercase;
        }

        public static OpType fromId(byte id) {
            switch (id) {
                case 0: return INDEX;
                case 1: return CREATE;
                case 2: return UPDATE;
                case 3: return DELETE;
                default: throw new IllegalArgumentException("Unknown opType: [" + id + "]");
            }
        }

        public static OpType fromString(String sOpType) {
            String lowerCase = sOpType.toLowerCase(Locale.ROOT);
            for (OpType opType : OpType.values()) {
                if (opType.getLowercase().equals(lowerCase)) {
                    return opType;
                }
            }
            throw new IllegalArgumentException("Unknown opType: [" + sOpType + "]");
        }
    }

    /** read a document write (index/delete/update) request */
    static DocWriteRequest readDocumentRequest(StreamInput in) throws IOException {
        byte type = in.readByte();
        DocWriteRequest docWriteRequest;
        if (type == 0) {
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.readFrom(in);
            docWriteRequest = indexRequest;
        } else if (type == 1) {
            DeleteRequest deleteRequest = new DeleteRequest();
            deleteRequest.readFrom(in);
            docWriteRequest = deleteRequest;
        } else if (type == 2) {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.readFrom(in);
            docWriteRequest = updateRequest;
        } else {
            throw new IllegalStateException("invalid request type [" + type+ " ]");
        }
        return docWriteRequest;
    }

    /** write a document write (index/delete/update) request*/
    static void writeDocumentRequest(StreamOutput out, DocWriteRequest request)  throws IOException {
        if (request instanceof IndexRequest) {
            out.writeByte((byte) 0);
            ((IndexRequest) request).writeTo(out);
        } else if (request instanceof DeleteRequest) {
            out.writeByte((byte) 1);
            ((DeleteRequest) request).writeTo(out);
        } else if (request instanceof UpdateRequest) {
            out.writeByte((byte) 2);
            ((UpdateRequest) request).writeTo(out);
        } else {
            throw new IllegalStateException("invalid request [" + request.getClass().getSimpleName() + " ]");
        }
    }

    /** Tests if the cluster is ready for compare and write using sequence numbers. */
    static boolean canUseIfSeqNo(ClusterState clusterState) {
        return clusterState.nodes().getMinNodeVersion().onOrAfter(Version.V_6_6_0);
    }

    static ActionRequestValidationException validateSeqNoBasedCASParams(
        DocWriteRequest request, ActionRequestValidationException validationException) {
        if (request.versionType().validateVersionForWrites(request.version()) == false) {
            validationException = addValidationError("illegal version value [" + request.version() + "] for version type ["
                + request.versionType().name() + "]", validationException);
        }
        if (request.versionType() == VersionType.FORCE) {
            validationException = addValidationError("version type [force] may no longer be used", validationException);
        }

        if (request.ifSeqNo() != UNASSIGNED_SEQ_NO && (
            request.versionType() != VersionType.INTERNAL || request.version() != Versions.MATCH_ANY
        )) {
            validationException = addValidationError("compare and write operations can not use versioning", validationException);
        }
        if (request.ifPrimaryTerm() == UNASSIGNED_PRIMARY_TERM && request.ifSeqNo() != UNASSIGNED_SEQ_NO) {
            validationException = addValidationError("ifSeqNo is set, but primary term is [0]", validationException);
        }
        if (request.ifPrimaryTerm() != UNASSIGNED_PRIMARY_TERM && request.ifSeqNo() == UNASSIGNED_SEQ_NO) {
            validationException =
                addValidationError("ifSeqNo is unassigned, but primary term is [" + request.ifPrimaryTerm() + "]", validationException);
        }

        return validationException;
    }
}
