package com.xaniihub.app.di;

import com.xaniihub.app.data.local.XaniiHubDatabase;
import com.xaniihub.app.data.local.dao.BodyDao;
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
public final class AppModule_ProvideBodyDaoFactory implements Factory<BodyDao> {
  private final Provider<XaniiHubDatabase> databaseProvider;

  public AppModule_ProvideBodyDaoFactory(Provider<XaniiHubDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public BodyDao get() {
    return provideBodyDao(databaseProvider.get());
  }

  public static AppModule_ProvideBodyDaoFactory create(
      Provider<XaniiHubDatabase> databaseProvider) {
    return new AppModule_ProvideBodyDaoFactory(databaseProvider);
  }

  public static BodyDao provideBodyDao(XaniiHubDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideBodyDao(database));
  }
}
