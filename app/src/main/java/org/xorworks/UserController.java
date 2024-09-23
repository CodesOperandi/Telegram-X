package org.xorworks;

import android.util.Log;

import androidx.annotation.Nullable;

import org.drinkless.tdlib.Client;
import org.drinkmore.tdlib.TdApi;
import org.thunderdog.challegram.telegram.Tdlib;

public class UserController implements Client.ResultHandler, Client.ExceptionHandler{

  private final Client tdlibClient;
  private final Tdlib.ResultHandler<TdApi.Object> meHandler;

  public UserController() {
    this.tdlibClient = Client.create(this, this, this);
  }

  public void getCurrentUserId() {
    Tdlib.ResultHandler<Tdlib.Object> resultHandler;
    resultHandler = new Tdlib.ResultHandler<Tdlib.Object>() {
      @Override
      public void onResult (Tdlib.Object result, @Nullable TdApi.Error error) {
        Log.i("xorwoks", "Result of GetMe " + result);
        Log.i("xorwoks", "Error of GetMe " + error);
      }
    };
    tdlibClient.send(new TdApi.GetMe(), resultHandler );
  }

  @Override
  public void onException (Throwable e) {
    Log.i("xorwoks", "Exception on client creation ", e)   ;
  }

  @Override
  public void onResult (TdApi.Object object) {
    Log.i("xorwoks", "Result on client creation ")   ;
  }
}
