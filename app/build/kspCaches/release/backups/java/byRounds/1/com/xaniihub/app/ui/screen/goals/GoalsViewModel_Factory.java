package com.xaniihub.app.ui.screen.goals;

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
public final class GoalsViewModel_Factory implements Factory<GoalsViewModel> {
  private final Provider<XaniiRepository> repositoryProvider;

  public GoalsViewModel_Factory(Provider<XaniiRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GoalsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static GoalsViewModel_Factory create(Provider<XaniiRepository> repositoryProvider) {
    return new GoalsViewModel_Factory(repositoryProvider);
  }

  public static GoalsViewModel newInstance(XaniiRepository repository) {
    return new GoalsViewModel(repository);
  }
}
