package com.jal.todo.module.addtask;

import android.annotation.SuppressLint;
import android.app.Application;
import android.text.TextUtils;

import com.jal.core.viewmodel.CustomViewModel;
import com.jal.todo.R;
import com.jal.todo.bean.RepeatTime;
import com.jal.todo.db.AppDatabase;
import com.jal.todo.db.entity.Task;
import com.jal.todo.widget.PickerDialog;

import org.jaaksi.pickerview.picker.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.databinding.ObservableList;
import jal.dev.common.utils.DateUtil;
import jal.dev.common.utils.ResourcesUtil;
import jal.dev.common.utils.ToastUtil;
import jal.dev.common.utils.rxjava.CommonRxTask;
import jal.dev.common.utils.rxjava.RxAdapter;
import me.goldze.mvvmhabit.binding.command.BindingAction;
import me.goldze.mvvmhabit.binding.command.BindingCommand;
import me.goldze.mvvmhabit.binding.command.BindingConsumer;
import me.goldze.mvvmhabit.bus.event.SingleLiveEvent;

public class AddTaskViewModel extends CustomViewModel {
    public ObservableField<String> mTaskContent = new ObservableField<>();
    public ObservableInt mSelectTime = new ObservableInt(R.id.today);
    public ObservableField<String> mDate = new ObservableField<>();
    public ObservableField<String> mEditSubTask = new ObservableField<>();
    public ObservableList<Task> mChildTasks = new ObservableArrayList<>();
    public ObservableBoolean mShowAddTask = new ObservableBoolean(false);
    public ObservableField<String> mRemindTime = new ObservableField<>();
    public ObservableField<RepeatTime> mRepeatTime = new ObservableField<>();
    public ObservableInt mPriority = new ObservableInt();
    public SingleLiveEvent<Calendar> showDatePicker = new SingleLiveEvent<>();
    public SingleLiveEvent<Void> showSelectRemindTimePicker = new SingleLiveEvent<>();
    private String mFullDate;
    private Calendar mCalendar = Calendar.getInstance();
    private Observable.OnPropertyChangedCallback changedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            mShowAddTask.set(!TextUtils.isEmpty(mEditSubTask.get()));
        }
    };

    public AddTaskViewModel(@NonNull Application application) {
        super(application);
        mEditSubTask.addOnPropertyChangedCallback(changedCallback);
    }

    public void setTask(Task task) {
        mTaskContent.set(task.content);
        if (task.subTaskList != null && task.subTaskList.size() > 0) {
            mChildTasks.addAll(task.subTaskList);
        }
        if (TextUtils.isEmpty(task.date)) {
            selectDate(null);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(DateUtil.parseTime(task.date, DateUtil.FormatType.yyyyMMdd));
            selectDate(calendar);
        }
        if(!TextUtils.isEmpty(task.remindTime)){
            mRemindTime.set(task.remindTime);
        }
    }

    public BindingCommand<Integer> mCheckedChangeCommand = new BindingCommand<>(new BindingConsumer<Integer>() {
        @Override
        public void call(Integer integer) {
            if (integer == R.id.today) {
                mCalendar = Calendar.getInstance();
                mDate.set(null);
            } else if (integer == R.id.tomorrow) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                mCalendar = calendar;
                mDate.set(null);
            } else {
                mCalendar = null;
                mDate.set(null);
            }
        }
    });
    public BindingCommand onSelectDataClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            showDatePicker.setValue(mCalendar);
        }
    });
    public BindingCommand onSelectRemindTimeClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            showSelectRemindTimePicker.call();
        }
    });
    public BindingCommand onAddSubTaskClick = new BindingCommand(new BindingAction() {
        @Override
        public void call() {
            mChildTasks.add(new Task(mEditSubTask.get()));
            mEditSubTask.set(null);
        }
    });
    public BindingCommand<Task> onTaskCheckChange = new BindingCommand<>(new BindingConsumer<Task>() {
        @Override
        public void call(Task task) {

        }
    });

    @Override
    public String getTitle() {
        return ResourcesUtil.getString(R.string.add_task);
    }

    @Override
    public BindingCommand getRightButtonClickCommand() {
        return new BindingCommand(new BindingAction() {
            @Override
            public void call() {
                saveTask();
            }
        });
    }

    @SuppressLint("CheckResult")
    private void saveTask() {
        if (TextUtils.isEmpty(mTaskContent.get())) {
            ToastUtil.showToast("请输入任务内容");
            return;
        }
        Task task = new Task();
        task.content = mTaskContent.get();
        task.date = DateUtil.formatDate(mCalendar.getTime(), DateUtil.FormatType.yyyyMMdd);
        task.remindTime=mRemindTime.get();
        String lastTask = mEditSubTask.get();
        if (mChildTasks.size() > 0 || !TextUtils.isEmpty(lastTask)) {
            task.subTaskList = new ArrayList<>();
            if (mChildTasks.size() > 0) {
                task.subTaskList.addAll(mChildTasks);
            }
            if (!TextUtils.isEmpty(lastTask)) {
                task.subTaskList.add(new Task(lastTask));
            }
        }

        RxAdapter.executeRxTask(new CommonRxTask<Task>(task) {
            @Override
            public void doInIOThread() {
                AppDatabase.getInstance(getApplication()).taskDao().insert(t);
                super.doInIOThread();
            }

            @Override
            public void doInUIThread() {
                finish();
                super.doInUIThread();
            }
        }, RxAdapter.<CommonRxTask<Task>>bindUntilEvent(getLifecycleProvider()));
    }

    private void selectTime(int id) {
        if (mSelectTime.get() == id) {
            mSelectTime.notifyChange();
        } else {
            mSelectTime.set(id);
        }
    }

    public void selectDate(Calendar calendar) {
        mCalendar = calendar;
        if (calendar == null) {
            selectTime(R.id.no_date);
        } else if (DateUtil.isToday(calendar.getTime())) {
            selectTime(R.id.today);
        } else if (DateUtil.isTomorrow(calendar.getTime())) {
            selectTime(R.id.tomorrow);
        } else {
            mDate.set(DateUtil.formatDate(calendar.getTime(), DateUtil.FormatType.MMdd).replace("-", "."));
            selectTime(-1);
        }
    }
    public void selectRemindTime(Date date){
        mRemindTime.set(DateUtil.formatDate(date,DateUtil.FormatType.HHmm).replace("-","."));
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        mEditSubTask.removeOnPropertyChangedCallback(changedCallback);
    }
}
