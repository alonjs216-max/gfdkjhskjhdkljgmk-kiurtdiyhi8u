package com.xaniihub.app.ui.screen.analytics;

import com.xaniihub.app.domain.repository.XaniiRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AnalyticsViewModel_Factory implements Factory<AnalyticsViewModel> {
  private final Provider<XaniiRepository> repositoryProvider;

  public AnalyticsViewModel_Factory(Provider<XaniiRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AnalyticsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static AnalyticsViewModel_Factory create(Provider<XaniiRepository> repositoryProvider) {
    return new AnalyticsViewModel_Factory(repositoryProvider);
  }

  public static AnalyticsViewModel newInstance(XaniiRepository repository) {
    return new AnalyticsViewModel(repository);
  }
}
