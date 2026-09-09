package com.xaniihub.app.di;

import com.xaniihub.app.data.local.XaniiHubDatabase;
import com.xaniihub.app.data.local.dao.GamificationDao;
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
public final class AppModule_ProvideGamificationDaoFactory implements Factory<GamificationDao> {
  private final Provider<XaniiHubDatabase> databaseProvider;

  public AppModule_ProvideGamificationDaoFactory(Provider<XaniiHubDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public GamificationDao get() {
    return provideGamificationDao(databaseProvider.get());
  }

  public static AppModule_ProvideGamificationDaoFactory create(
      Provider<XaniiHubDatabase> databaseProvider) {
    return new AppModule_ProvideGamificationDaoFactory(databaseProvider);
  }

  public static GamificationDao provideGamificationDao(XaniiHubDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGamificationDao(database));
  }
}
