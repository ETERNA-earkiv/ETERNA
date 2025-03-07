/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/keeps/roda
 */
package org.roda.wui.client.common;

import elemental2.promise.Promise;
import org.roda.wui.client.common.utils.AsyncCallbackUtils;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class PromiseAsyncCallback<T> implements AsyncCallback<T> {

  private Promise.PromiseExecutorCallbackFn.ResolveCallbackFn<T> resolveFunc;
  private Promise.PromiseExecutorCallbackFn.RejectCallbackFn rejectFunc;

  private final Promise<T> promise = new Promise<T>(
          (resolve, reject) -> {
            resolveFunc = resolve;
            rejectFunc = reject;
          }
  );

  public Promise<T> getPromise() {
    return promise;
  }

  @Override
  public void onFailure(Throwable caught) {
    rejectFunc.onInvoke(caught);
  }

  @Override
  public void onSuccess(T result) {
    resolveFunc.onInvoke(result);
  }
}
