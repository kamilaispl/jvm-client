package net.ravendb.tests.issues;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import net.ravendb.abstractions.data.SubscriptionConfig;
import net.ravendb.abstractions.data.SubscriptionConnectionOptions;
import net.ravendb.abstractions.data.SubscriptionCriteria;
import net.ravendb.abstractions.exceptions.subscriptions.SubscriptionDoesNotExistExeption;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.RemoteClientTest;
import net.ravendb.client.document.DocumentStore;


public class RavenDB_2627 extends RemoteClientTest {

  @Test
  public void canCreateSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.getSubscriptions().create(new SubscriptionCriteria());
      assertEquals(1, id);

      id = store.getSubscriptions().create(new SubscriptionCriteria());
      assertEquals(2, id);
    }
  }

  @Test
  public void canDeleteSubscription() throws Exception {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id1 = store.getSubscriptions().create(new SubscriptionCriteria());
      long id2 = store.getSubscriptions().create(new SubscriptionCriteria());

      List<SubscriptionConfig> subscriptions = store.getSubscriptions().getSubscriptions(0, 5);

      assertEquals(2, subscriptions.size());

      store.getSubscriptions().delete(id1);
      store.getSubscriptions().delete(id2);

      subscriptions = store.getSubscriptions().getSubscriptions(0, 5);

      assertEquals(0, subscriptions.size());
    }
  }

  @Test
  public void shouldThrowWhenOpeningNoExisingSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      try {
        store.getSubscriptions().open(1, new SubscriptionConnectionOptions());
        fail();
      } catch (SubscriptionDoesNotExistExeption e) {
        assertEquals("There is no subscription configuration for specified identifier (id: 1)", e.getMessage());
      }
    }
  }

  @Test
  public void shouldThrowOnAttemptToOpenAlreadyOpenedSubscription() {
    try (IDocumentStore store = new DocumentStore(getDefaultUrl(), getDefaultDb()).initialize()) {
      long id = store.getSubscriptions().create(new SubscriptionCriteria());
      store.getSubscriptions().open(1, new SubscriptionConnectionOptions());

      try {
        store.getSubscriptions().open(1, new SubscriptionConnectionOptions());
        fail();
      } catch (SubscriptionDoesNotExistExeption e) {
        assertEquals("Subscription is already in use. There can be only a single open subscription connection per subscription.", e.getMessage());
      }
    }
  }

  /*
   * private readonly TimeSpan waitForDocTimeout = TimeSpan.FromSeconds(20);



        [Fact]
        public void ShouldThrowOnAttemptToOpenAlreadyOpenedSubscription()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                store.Subscriptions.Open(id, new SubscriptionConnectionOptions());

                var ex = Assert.Throws<SubscriptionInUseException>(() => store.Subscriptions.Open(id, new SubscriptionConnectionOptions()));
                Assert.Equal("Subscription is already in use. There can be only a single open subscription connection per subscription.", ex.Message);
            }
        }

        [Fact]
        public void ShouldStreamAllDocumentsAfterSubscriptionCreation()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    session.Store(new User { Age = 31}, "users/1");
                    session.Store(new User { Age = 27}, "users/12");
                    session.Store(new User { Age = 25}, "users/3");

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());

                var keys = new BlockingCollection<string>();
                var ages = new BlockingCollection<int>();

                subscription.Subscribe(x => keys.Add(x[Constants.Metadata].Value<string>("@id")));
                subscription.Subscribe(x => ages.Add(x.Value<int>("Age")));

                string key;
                Assert.True(keys.TryTake(out key, waitForDocTimeout));
                Assert.Equal("users/1", key);

                Assert.True(keys.TryTake(out key, waitForDocTimeout));
                Assert.Equal("users/12", key);

                Assert.True(keys.TryTake(out key, waitForDocTimeout));
                Assert.Equal("users/3", key);

                int age;
                Assert.True(ages.TryTake(out age, waitForDocTimeout));
                Assert.Equal(31, age);

                Assert.True(ages.TryTake(out age, waitForDocTimeout));
                Assert.Equal(27, age);

                Assert.True(ages.TryTake(out age, waitForDocTimeout));
                Assert.Equal(25, age);
            }
        }

        [Fact]
        public void ShouldSendAllNewAndModifiedDocs()
        {
            using (var store = NewRemoteDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());

                var names = new BlockingCollection<string>();
                store.Changes().WaitForAllPendingSubscriptions();

                subscription.Subscribe(x => names.Add(x.Value<string>("Name")));

                using (var session = store.OpenSession())
                {
                    session.Store(new User { Name = "James" }, "users/1");
                    session.SaveChanges();
                }

                string name;
                Assert.True(names.TryTake(out name, waitForDocTimeout));
                Assert.Equal("James", name);

                using (var session = store.OpenSession())
                {
                    session.Store(new User { Name = "Adam"}, "users/12");
                    session.SaveChanges();
                }

                Assert.True(names.TryTake(out name, waitForDocTimeout));
                Assert.Equal("Adam", name);

                using (var session = store.OpenSession())
                {
                    session.Store(new User { Name = "David"}, "users/1");
                    session.SaveChanges();
                }

                Assert.True(names.TryTake(out name, waitForDocTimeout));
                Assert.Equal("David", name);
            }
        }

        [Fact]
        public void ShouldResendDocsIfAcknowledgmentTimeoutOccurred()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscriptionZeroTimeout = store.Subscriptions.Open(id, new SubscriptionConnectionOptions
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        AcknowledgmentTimeout = TimeSpan.FromMilliseconds(-10) // the client won't be able to acknowledge in negative time
                    }
                });
                store.Changes().WaitForAllPendingSubscriptions();
                var docs = new BlockingCollection<RavenJObject>();

                subscriptionZeroTimeout.Subscribe(docs.Add);

                using (var session = store.OpenSession())
                {
                    session.Store(new User {Name = "Raven"});
                    session.SaveChanges();
                }

                RavenJObject document;

                Assert.True(docs.TryTake(out document, waitForDocTimeout));
                Assert.Equal("Raven", document.Value<string>("Name"));

                Assert.True(docs.TryTake(out document, waitForDocTimeout));
                Assert.Equal("Raven", document.Value<string>("Name"));

                Assert.True(docs.TryTake(out document, waitForDocTimeout));
                Assert.Equal("Raven", document.Value<string>("Name"));

                subscriptionZeroTimeout.Dispose();

                // retry with longer timeout - should sent just one doc

                var subscriptionLongerTimeout = store.Subscriptions.Open(id, new SubscriptionConnectionOptions
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        AcknowledgmentTimeout = TimeSpan.FromSeconds(30)
                    }
                });

                var docs2 = new BlockingCollection<RavenJObject>();

                subscriptionLongerTimeout.Subscribe(docs2.Add);

                Assert.True(docs2.TryTake(out document, waitForDocTimeout));
                Assert.Equal("Raven", document.Value<string>("Name"));

                Assert.False(docs2.TryTake(out document, waitForDocTimeout));
            }
        }


        [Fact]
        public void ShouldRespectMaxDocCountInBatch()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 100; i++)
                    {
                        session.Store(new Company());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions{ BatchOptions = new SubscriptionBatchOptions { MaxDocCount = 25 }});

                var batchSizes = new List<Reference<int>>();

                subscription.BeforeBatch +=
                    () => batchSizes.Add(new Reference<int>());

                subscription.Subscribe(x =>
                {
                    var reference = batchSizes.Last();
                    reference.Value++;
                });

                var result = SpinWait.SpinUntil(() => batchSizes.ToList().Sum(x => x.Value) >= 100, TimeSpan.FromSeconds(60));

                Assert.True(result);

                Assert.Equal(4, batchSizes.Count);

                foreach (var reference in batchSizes)
                {
                    Assert.Equal(25, reference.Value);
                }
            }
        }

        [Fact]
        public void ShouldRespectMaxBatchSize()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 100; i++)
                    {
                        session.Store(new Company());
                        session.Store(new User());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions()
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        MaxSize = 16 * 1024
                    }
                });

                var batches = new List<List<RavenJObject>>();

                subscription.BeforeBatch +=
                    () => batches.Add(new List<RavenJObject>());

                subscription.Subscribe(x =>
                {
                    var list = batches.Last();
                    list.Add(x);
                });

                var result = SpinWait.SpinUntil(() => batches.ToList().Sum(x => x.Count) >= 200, TimeSpan.FromSeconds(60));

                Assert.True(result);
                Assert.True(batches.Count > 1);
            }
        }

        [Fact]
        public void ShouldRespectCollectionCriteria()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 100; i++)
                    {
                        session.Store(new Company());
                        session.Store(new User());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria
                {
                    BelongsToCollection = "Users"
                });

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions
                {
                    BatchOptions = new SubscriptionBatchOptions { MaxDocCount = 31 }
                });

                var docs = new List<RavenJObject>();

                subscription.Subscribe(docs.Add);

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 100, TimeSpan.FromSeconds(60)));


                foreach (var jsonDocument in docs)
                {
                    Assert.Equal("Users", jsonDocument[Constants.Metadata].Value<string>(Constants.RavenEntityName));
                }
            }
        }

        [Fact]
        public void ShouldRespectStartsWithCriteria()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 100; i++)
                    {
                        session.Store(new User(), i % 2 == 0 ? "users/" : "users/favorite/");
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria
                {
                    KeyStartsWith = "users/favorite/"
                });

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        MaxDocCount = 15
                    }
                });

                var docs = new List<RavenJObject>();

                subscription.Subscribe(docs.Add);

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 50, TimeSpan.FromSeconds(60)));


                foreach (var jsonDocument in docs)
                {
                    Assert.True(jsonDocument[Constants.Metadata].Value<string>("@id").StartsWith("users/favorite/"));
                }
            }
        }

        [Fact]
        public void ShouldRespectPropertiesCriteria()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 10; i++)
                    {
                        session.Store(new User
                        {
                            Name = i % 2 == 0 ? "Jessica" : "Caroline"
                        });

                        session.Store(new Person
                        {
                            Name = i % 2 == 0 ? "Caroline" : "Samantha"
                        });

                        session.Store(new Company());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria
                {
                    PropertiesMatch = new Dictionary<string, RavenJToken>()
                    {
                        {"Name", "Caroline"}
                    }
                });

                var carolines = store.Subscriptions.Open(id, new SubscriptionConnectionOptions { BatchOptions = new SubscriptionBatchOptions { MaxDocCount = 5 }});

                var docs = new List<RavenJObject>();

                carolines.Subscribe(docs.Add);

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 10, TimeSpan.FromSeconds(60)));


                foreach (var jsonDocument in docs)
                {
                    Assert.Equal("Caroline", jsonDocument.Value<string>("Name"));
                }
            }
        }

        [Fact]
        public void ShouldRespectPropertiesNotMatchCriteria()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 10; i++)
                    {
                        session.Store(new User
                        {
                            Name = i % 2 == 0 ? "Jessica" : "Caroline"
                        });

                        session.Store(new Person
                        {
                            Name = i % 2 == 0 ? "Caroline" : "Samantha"
                        });

                        session.Store(new Company());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria
                {
                    PropertiesNotMatch = new Dictionary<string, RavenJToken>()
                    {
                        {"Name", "Caroline"}
                    }
                });

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        MaxDocCount = 5
                    }
                });

                var docs = new List<RavenJObject>();

                subscription.Subscribe(docs.Add);

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 20, TimeSpan.FromSeconds(60)));


                foreach (var jsonDocument in docs)
                {
                    Assert.True(jsonDocument.ContainsKey("Name") == false || jsonDocument.Value<string>("Name") != "Caroline");
                }
            }
        }

        [Fact]
        public void CanGetSubscriptionsFromDatabase()
        {
            using (var store = NewDocumentStore())
            {
                var subscriptionDocuments = store.Subscriptions.GetSubscriptions(0, 10);

                Assert.Equal(0, subscriptionDocuments.Count);

                store.Subscriptions.Create(new SubscriptionCriteria
                {
                    KeyStartsWith = "users/"
                });

                subscriptionDocuments = store.Subscriptions.GetSubscriptions(0, 10);

                Assert.Equal(1, subscriptionDocuments.Count);
                Assert.Equal("users/", subscriptionDocuments[0].Criteria.KeyStartsWith);

                var subscription = store.Subscriptions.Open(subscriptionDocuments[0].SubscriptionId, new SubscriptionConnectionOptions());

                var docs = new List<RavenJObject>();
                subscription.Subscribe(docs.Add);

                using (var session = store.OpenSession())
                {
                    session.Store(new User());
                    session.SaveChanges();
                }

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 1, TimeSpan.FromSeconds(60)));
            }
        }

        [Fact]
        public void ShouldKeepPullingDocsAfterServerRestart()
        {
            var dataPath = NewDataPath("RavenDB_2627_after_restart");

            IDocumentStore store = null;
            try
            {
                var serverDisposed = false;

                var server = GetNewServer(dataDirectory: dataPath, runInMemory: false);

                store = new DocumentStore()
                {
                    Url = "http://localhost:" + server.Configuration.Port,
                    DefaultDatabase = "RavenDB_2627"
                }.Initialize();

                using (var session = store.OpenSession())
                {
                    session.Store(new User());
                    session.Store(new User());
                    session.Store(new User());
                    session.Store(new User());

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria());

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions()
                {
                    BatchOptions = new SubscriptionBatchOptions()
                    {
                        MaxDocCount = 1
                    }
                });
                store.Changes().WaitForAllPendingSubscriptions();

                var serverDisposingHandler = subscription.Subscribe(x =>
                {
                    server.Dispose(); // dispose the server
                    serverDisposed = true;
                });

                SpinWait.SpinUntil(() => serverDisposed, TimeSpan.FromSeconds(30));

                serverDisposingHandler.Dispose();

                var docs = new BlockingCollection<RavenJObject>();
                subscription.Subscribe(docs.Add);

                //recreate the server
                GetNewServer(dataDirectory: dataPath, runInMemory: false);

                RavenJObject doc;
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));

                using (var session = store.OpenSession())
                {
                    session.Store(new User(), "users/arek");
                    session.SaveChanges();
                }

                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.Equal("users/arek", doc[Constants.Metadata].Value<string>("@id"));
            }
            finally
            {
                if(store != null)
                    store.Dispose();
            }
        }

        [Fact]
        public void ShouldStopPullingDocsIfThereIsNoSubscriber()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());
                store.Changes().WaitForAllPendingSubscriptions();

                using (var session = store.OpenSession())
                {
                    session.Store(new User(), "users/1");
                    session.Store(new User(), "users/2");
                    session.SaveChanges();
                }

                var docs = new BlockingCollection<RavenJObject>();
                var subscribe = subscription.Subscribe(docs.Add);

                RavenJObject doc;
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));

                subscribe.Dispose();

                using (var session = store.OpenSession())
                {
                    session.Store(new User(), "users/3");
                    session.Store(new User(), "users/4");
                    session.SaveChanges();
                }

                Thread.Sleep(TimeSpan.FromSeconds(5)); // should not pull any docs because there is no subscriber that could process them

                subscription.Subscribe(docs.Add);

                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.Equal("users/3", doc[Constants.Metadata].Value<string>("@id"));

                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.Equal("users/4", doc[Constants.Metadata].Value<string>("@id"));
            }
        }

        [Fact]
        public void ShouldAllowToOpenSubscriptionIfClientDidntSentAliveNotificationOnTime()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    session.Store(new User());
                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria());

                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions()
                {
                    ClientAliveNotificationInterval = TimeSpan.FromSeconds(2)
                });
                store.Changes().WaitForAllPendingSubscriptions();

                subscription.AfterBatch += () => Thread.Sleep(TimeSpan.FromSeconds(20)); // to prevent the client from sending client-alive notification

                var docs = new BlockingCollection<RavenJObject>();

                subscription.Subscribe(docs.Add);
                store.Changes().WaitForAllPendingSubscriptions();

                RavenJObject _;
                Assert.True(docs.TryTake(out _, waitForDocTimeout));

                Thread.Sleep(TimeSpan.FromSeconds(10));

                // first open subscription didn't send the client-alive notification in time, so the server should allow to open it for this subscription
                var newSubscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());

                var docs2 = new BlockingCollection<RavenJObject>();
                newSubscription.Subscribe(docs2.Add);

                using (var session = store.OpenSession())
                {
                    session.Store(new User());
                    session.SaveChanges();
                }

                Assert.True(docs2.TryTake(out _, waitForDocTimeout));

                Assert.False(docs.TryTake(out _, TimeSpan.FromSeconds(2))); // make sure that first subscriber didn't get new doc
            }
        }

        [Fact]
        public void CanReleaseSubscription()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                store.Subscriptions.Open(id, new SubscriptionConnectionOptions());
                store.Changes().WaitForAllPendingSubscriptions();

                Assert.Throws<SubscriptionInUseException>(() => store.Subscriptions.Open(id, new SubscriptionConnectionOptions()));

                store.Subscriptions.Release(id);

                Assert.DoesNotThrow(() => store.Subscriptions.Open(id, new SubscriptionConnectionOptions()));
            }
        }

        [Fact]
        public void ShouldPullDocumentsAfterBulkInsert()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());
                store.Changes().WaitForAllPendingSubscriptions();

                var docs = new BlockingCollection<RavenJObject>();

                subscription.Subscribe(docs.Add);

                store.Changes().WaitForAllPendingSubscriptions();

                using (var bulk = store.BulkInsert())
                {
                    bulk.Store(new User());
                    bulk.Store(new User());
                    bulk.Store(new User());
                }

                RavenJObject doc;
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
            }
        }

        [Fact]
        public void ShouldStopPullingDocsAndCloseSubscriptionOnSubscriberErrorByDefault()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions());

                var docs = new BlockingCollection<RavenJObject>();

                var subscriberException = new TaskCompletionSource<object>();

                subscription.Subscribe(docs.Add);
                subscription.Subscribe(x =>
                {
                    throw new Exception("Fake exception");
                },
                ex => subscriberException.TrySetResult(ex));

                store.Changes().WaitForAllPendingSubscriptions();

                store.DatabaseCommands.Put("items/1", null, new RavenJObject(), new RavenJObject());

                Assert.True(subscriberException.Task.Wait(waitForDocTimeout));
                Assert.True(subscription.IsErrored);

                Assert.True(SpinWait.SpinUntil(() => subscription.IsClosed, waitForDocTimeout));

                var subscriptionConfig = store.Subscriptions.GetSubscriptions(0, 1).First();

                Assert.Equal(Etag.Empty, subscriptionConfig.AckEtag);
            }
        }

        [Fact]
        public void CanSetToIgnoreSubscriberErrors()
        {
            using (var store = NewDocumentStore())
            {
                var id = store.Subscriptions.Create(new SubscriptionCriteria());
                var subscription = store.Subscriptions.Open(id, new SubscriptionConnectionOptions()
                {
                    IgnoreSubscribersErrors = true
                });
                store.Changes().WaitForAllPendingSubscriptions();

                var docs = new BlockingCollection<RavenJObject>();

                subscription.Subscribe(docs.Add);
                subscription.Subscribe(x =>
                {
                    throw new Exception("Fake exception");
                });

                store.Changes().WaitForAllPendingSubscriptions();

                store.DatabaseCommands.Put("items/1", null, new RavenJObject(), new RavenJObject());
                store.DatabaseCommands.Put("items/2", null, new RavenJObject(), new RavenJObject());

                RavenJObject doc;
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.True(docs.TryTake(out doc, waitForDocTimeout));
                Assert.False(subscription.IsErrored);
            }
        }

        [Fact]
        public void CanUseNestedPropertiesInSubscriptionCriteria()
        {
            using (var store = NewDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    for (int i = 0; i < 10; i++)
                    {
                        session.Store(new PersonWithAddress
                        {
                            Address = new Address()
                            {
                                Street = "1st Street",
                                ZipCode = i % 2 == 0 ? 999 : 12345
                            }
                        });

                        session.Store(new PersonWithAddress
                        {
                            Address = new Address()
                            {
                                Street = "2nd Street",
                                ZipCode = 12345
                            }
                        });

                        session.Store(new Company());
                    }

                    session.SaveChanges();
                }

                var id = store.Subscriptions.Create(new SubscriptionCriteria
                {
                    PropertiesMatch = new Dictionary<string, RavenJToken>()
                    {
                        {"Address.Street", "1st Street"}
                    },
                    PropertiesNotMatch = new Dictionary<string, RavenJToken>()
                    {
                        {"Address.ZipCode", 999}
                    }
                });

                var carolines = store.Subscriptions.Open(id, new SubscriptionConnectionOptions { BatchOptions = new SubscriptionBatchOptions { MaxDocCount = 5 } });

                var docs = new List<RavenJObject>();

                carolines.Subscribe(docs.Add);

                Assert.True(SpinWait.SpinUntil(() => docs.Count >= 5, TimeSpan.FromSeconds(60)));


                foreach (var jsonDocument in docs)
                {
                    Assert.Equal("1st Street", jsonDocument.Value<RavenJObject>("Address").Value<string>("Street"));
                }
            }
        }
   */
}
