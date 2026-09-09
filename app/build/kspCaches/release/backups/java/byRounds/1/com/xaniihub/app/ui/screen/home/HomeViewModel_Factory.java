package com.xaniihub.app.ui.screen.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<XaniiRepository> repositoryProvider;

  public HomeViewModel_Factory(Provider<XaniiRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<XaniiRepository> repositoryProvider) {
    return new HomeViewModel_Factory(repositoryProvider);
  }

  public static HomeViewModel newInstance(XaniiRepository repository) {
    return new HomeViewModel(repository);
  }
}
