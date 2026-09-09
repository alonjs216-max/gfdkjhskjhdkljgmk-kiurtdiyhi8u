package com.xaniihub.app.data.repository;

import android.content.Context;
import com.xaniihub.app.data.local.dao.BodyDao;
import com.xaniihub.app.data.local.dao.GamificationDao;
import com.xaniihub.app.data.local.dao.GoalDao;
import com.xaniihub.app.data.local.dao.StepDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class XaniiRepositoryImpl_Factory implements Factory<XaniiRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<StepDao> stepDaoProvider;

  private final Provider<GoalDao> goalDaoProvider;

  private final Provider<BodyDao> bodyDaoProvider;

  private final Provider<GamificationDao> gamificationDaoProvider;

  public XaniiRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<StepDao> stepDaoProvider, Provider<GoalDao> goalDaoProvider,
      Provider<BodyDao> bodyDaoProvider, Provider<GamificationDao> gamificationDaoProvider) {
    this.contextProvider = contextProvider;
    this.stepDaoProvider = stepDaoProvider;
    this.goalDaoProvider = goalDaoProvider;
    this.bodyDaoProvider = bodyDaoProvider;
    this.gamificationDaoProvider = gamificationDaoProvider;
  }

  @Override
  public XaniiRepositoryImpl get() {
    return newInstance(contextProvider.get(), stepDaoProvider.get(), goalDaoProvider.get(), bodyDaoProvider.get(), gamificationDaoProvider.get());
  }

  public static XaniiRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<StepDao> stepDaoProvider, Provider<GoalDao> goalDaoProvider,
      Provider<BodyDao> bodyDaoProvider, Provider<GamificationDao> gamificationDaoProvider) {
    return new XaniiRepositoryImpl_Factory(contextProvider, stepDaoProvider, goalDaoProvider, bodyDaoProvider, gamificationDaoProvider);
  }

  public static XaniiRepositoryImpl newInstance(Context context, StepDao stepDao, GoalDao goalDao,
      BodyDao bodyDao, GamificationDao gamificationDao) {
    return new XaniiRepositoryImpl(context, stepDao, goalDao, bodyDao, gamificationDao);
  }
}
