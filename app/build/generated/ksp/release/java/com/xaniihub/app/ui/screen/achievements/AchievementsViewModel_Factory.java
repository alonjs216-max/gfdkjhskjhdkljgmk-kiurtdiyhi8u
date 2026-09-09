package com.xaniihub.app.ui.screen.achievements;

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
public final class AchievementsViewModel_Factory implements Factory<AchievementsViewModel> {
  private final Provider<XaniiRepository> repositoryProvider;

  public AchievementsViewModel_Factory(Provider<XaniiRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AchievementsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static AchievementsViewModel_Factory create(Provider<XaniiRepository> repositoryProvider) {
    return new AchievementsViewModel_Factory(repositoryProvider);
  }

  public static AchievementsViewModel newInstance(XaniiRepository repository) {
    return new AchievementsViewModel(repository);
  }
}
