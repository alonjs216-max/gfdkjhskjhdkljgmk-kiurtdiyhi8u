package com.xaniihub.app.tracking;

import com.xaniihub.app.domain.repository.XaniiRepository;
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
public final class StepTrackingService_MembersInjector implements MembersInjector<StepTrackingService> {
  private final Provider<XaniiRepository> repositoryProvider;

  public StepTrackingService_MembersInjector(Provider<XaniiRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public static MembersInjector<StepTrackingService> create(
      Provider<XaniiRepository> repositoryProvider) {
    return new StepTrackingService_MembersInjector(repositoryProvider);
  }

  @Override
  public void injectMembers(StepTrackingService instance) {
    injectRepository(instance, repositoryProvider.get());
  }

  @InjectedFieldSignature("com.xaniihub.app.tracking.StepTrackingService.repository")
  public static void injectRepository(StepTrackingService instance, XaniiRepository repository) {
    instance.repository = repository;
  }
}
