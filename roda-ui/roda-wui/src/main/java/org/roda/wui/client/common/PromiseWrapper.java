package org.roda.wui.client.common;

import elemental2.promise.IThenable;
import elemental2.promise.Promise;

public class PromiseWrapper<T> {
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

  public IThenable<Void> resolve(T value) {
    resolveFunc.onInvoke(value);
    return null;
  }

  public IThenable<Void> reject(Object reason) {
    rejectFunc.onInvoke(reason);
    return null;
  }
}
