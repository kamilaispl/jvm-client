package net.ravendb.client.utils;

import java.util.Date;

import net.ravendb.abstractions.exceptions.OperationCancelledException;


public class CancellationTokenSource {

  boolean cancelled = false;

  protected Long cancelAfterDate;

  public CancellationToken getToken() {
    return new CancellationToken();
  }

  public class CancellationToken {
    protected CancellationToken() {

    }

    public boolean isCancellationRequested() {
      return cancelled || (cancelAfterDate != null && new Date().getTime() > cancelAfterDate);
    }

    public void throwIfCancellationRequested() {
      if (isCancellationRequested()) {
        throw new OperationCancelledException();
      }
    }
  }

  public void cancel() {
    cancelled = true;
  }

  public void cancelAfter(long timeoutInMilis) {
    this.cancelAfterDate = new Date().getTime() + timeoutInMilis;
  }
}
