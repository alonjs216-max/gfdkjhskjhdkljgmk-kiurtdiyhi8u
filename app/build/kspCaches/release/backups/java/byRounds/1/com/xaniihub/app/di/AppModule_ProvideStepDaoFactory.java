package com.xaniihub.app.di;

import com.xaniihub.app.data.local.XaniiHubDatabase;
import com.xaniihub.app.data.local.dao.StepDao;
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
public final class AppModule_ProvideStepDaoFactory implements Factory<StepDao> {
  private final Provider<XaniiHubDatabase> databaseProvider;

  public AppModule_ProvideStepDaoFactory(Provider<XaniiHubDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public StepDao get() {
    return provideStepDao(databaseProvider.get());
  }

  public static AppModule_ProvideStepDaoFactory create(
      Provider<XaniiHubDatabase> databaseProvider) {
    return new AppModule_ProvideStepDaoFactory(databaseProvider);
  }

  public static StepDao provideStepDao(XaniiHubDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideStepDao(database));
  }
}
