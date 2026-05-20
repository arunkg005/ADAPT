package com.example.adapt.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adapt.R;
import com.example.adapt.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private final List<ChatMessage> messages = new ArrayList<>();

    public void submitList(List<ChatMessage> nextMessages) {
        messages.clear();
        if (nextMessages != null) {
            messages.addAll(nextMessages);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.user_message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getContent());
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;

        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.bot_message_text);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getContent());
        }
    }
}
