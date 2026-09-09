package com.xaniihub.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class XaniiHubApp_MembersInjector implements MembersInjector<XaniiHubApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public XaniiHubApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<XaniiHubApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new XaniiHubApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(XaniiHubApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.xaniihub.app.XaniiHubApp.workerFactory")
  public static void injectWorkerFactory(XaniiHubApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
