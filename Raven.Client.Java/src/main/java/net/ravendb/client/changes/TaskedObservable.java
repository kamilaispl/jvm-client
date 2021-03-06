package net.ravendb.client.changes;

import java.io.Closeable;
import java.io.IOException;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.abstractions.closure.Predicates;
import net.ravendb.client.connection.profiling.ConcurrentSet;



public class TaskedObservable<T, TConnectionState extends IChangesConnectionState> implements IObservable<T> {
  protected final TConnectionState localConnectionState;
  protected Predicate<T> filter;
  protected ConcurrentSet<IObserver<T>> subscribers = new ConcurrentSet<>();

  public TaskedObservable(TConnectionState localConnectionState, Predicate<T> filter) {
    this.localConnectionState = localConnectionState;
    this.filter = filter;
  }

  @Override
  public CleanCloseable subscribe(final IObserver<T> observer) {
    localConnectionState.inc();
    subscribers.add(observer);
    return new CleanCloseable() {

      @Override
      public void close() {
        localConnectionState.dec();
        subscribers.remove(observer);
      }
    };
  }

  public void send(T msg) {
    try {
      if (!filter.apply(msg)) {
        return;
      }
    } catch (Exception e) {
      error(e);
      return;
    }

    for (IObserver<T> subscriber : subscribers) {
      subscriber.onNext(msg);
    }
  }

  public void error(Exception obj) {
    for (IObserver<T> subscriber : subscribers) {
      subscriber.onError(obj);
    }
  }

  @Override
  public IObservable<T> where(Predicate<T> predicate) {
    filter = Predicates.and(filter, predicate);
    return this;
  }

}
