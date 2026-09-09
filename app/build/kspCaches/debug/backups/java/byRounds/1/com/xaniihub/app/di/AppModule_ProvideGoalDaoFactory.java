package com.xaniihub.app.di;

import com.xaniihub.app.data.local.XaniiHubDatabase;
import com.xaniihub.app.data.local.dao.GoalDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideGoalDaoFactory implements Factory<GoalDao> {
  private final Provider<XaniiHubDatabase> databaseProvider;

  public AppModule_ProvideGoalDaoFactory(Provider<XaniiHubDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GoalDao get() {
    return provideGoalDao(databaseProvider.get());
  }

  public static AppModule_ProvideGoalDaoFactory create(
      Provider<XaniiHubDatabase> databaseProvider) {
    return new AppModule_ProvideGoalDaoFactory(databaseProvider);
  }

  public static GoalDao provideGoalDao(XaniiHubDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGoalDao(database));
  }
}
