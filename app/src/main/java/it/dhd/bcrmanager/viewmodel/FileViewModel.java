package it.dhd.bcrmanager.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import it.dhd.bcrmanager.objects.CallLogItem;

public class FileViewModel extends AndroidViewModel {
    private final MutableLiveData<DataWrapper> dataList = new MutableLiveData<>();
    private final MutableLiveData<String> parsingError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<CallLogItem> playingItem = new MutableLiveData<>();
    private final MutableLiveData<Integer> deletedItems = new MutableLiveData<>();
    private DataRepository dataRepository;

    public FileViewModel(@NonNull Application application) {
        super(application);
        if (dataRepository == null) {
            dataRepository = DataRepository.getInstance();
        }
    }

    public LiveData<DataWrapper> getDataList() {
        return dataList;
    }

    public MutableLiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void setPlayingItem(CallLogItem item) {
        playingItem.setValue(item);
    }

    public MutableLiveData<CallLogItem> getPlayingItem() {
        return playingItem;
    }

    public MutableLiveData<Integer> getDeletedItems() {
        return deletedItems;
    }

    public double getMaxDuration() {
        if (dataList.getValue() != null) return dataList.getValue().maxDuration();
        else return 0;
    }

    public void fetchData(Context c, String lookupKey) {
        isLoading.setValue(true);
        Disposable disposable = dataRepository.fetchData(c, lookupKey)
                .subscribe(newData -> {
                            isLoading.setValue(false);
                            dataList.setValue(newData);
                        },
                        throwable -> {
                            isLoading.setValue(false);
                            StringWriter sw = new StringWriter();
                            throwable.printStackTrace(new PrintWriter(sw));
                            parsingError.setValue(sw.toString());
                        }
                );
    }

    public MutableLiveData<String> getParsingError() {
        return parsingError;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void deleteItems(Context context, List<CallLogItem> registrationsItems) {
        isLoading.setValue(true);
        Disposable disposable = dataRepository.removeItems(context, registrationsItems)
                .subscribe(newData -> {
                            isLoading.setValue(false);
                            dataList.setValue(newData);
                            deletedItems.setValue(registrationsItems.size());
                        },
                        throwable -> {
                            isLoading.setValue(false);
                            StringWriter sw = new StringWriter();
                            throwable.printStackTrace(new PrintWriter(sw));
                            parsingError.setValue(sw.toString());
                        }
                );
    }

    public List<Object> getSortedItems() {
        if (dataList.getValue() != null) return dataList.getValue().sortedListWithHeaders();
        else return new ArrayList<>();
    }
}
