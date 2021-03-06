/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.watcher.transport.actions.delete;

import java.util.function.Supplier;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchAction;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.xpack.core.watcher.watch.Watch;

import static org.elasticsearch.xpack.core.ClientHelper.WATCHER_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

/**
 * Performs the delete operation. This inherits directly from HandledTransportAction, because deletion should always work
 * independently from the license check in WatcherTransportAction!
 */
public class TransportDeleteWatchAction extends HandledTransportAction<DeleteWatchRequest, DeleteWatchResponse> {

    private final Client client;

    @Inject
    public TransportDeleteWatchAction(Settings settings, TransportService transportService,ThreadPool threadPool,
                                      ActionFilters actionFilters, Client client) {
        super(settings, DeleteWatchAction.NAME, threadPool, transportService, actionFilters,
            (Supplier<DeleteWatchRequest>) DeleteWatchRequest::new);
        this.client = client;
    }

    @Override
    protected void doExecute(DeleteWatchRequest request, ActionListener<DeleteWatchResponse> listener) {
        DeleteRequest deleteRequest = new DeleteRequest(Watch.INDEX, Watch.DOC_TYPE, request.getId());
        deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        executeAsyncWithOrigin(client.threadPool().getThreadContext(), WATCHER_ORIGIN, deleteRequest,
                ActionListener.<DeleteResponse>wrap(deleteResponse -> {
                    boolean deleted = deleteResponse.getResult() == DocWriteResponse.Result.DELETED;
                    DeleteWatchResponse response = new DeleteWatchResponse(deleteResponse.getId(), deleteResponse.getVersion(), deleted);
                    listener.onResponse(response);
                }, listener::onFailure), client::delete);
    }
}
