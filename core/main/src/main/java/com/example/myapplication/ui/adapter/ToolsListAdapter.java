package com.example.myapplication.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.rk.terminal.R;
import com.example.myapplication.services.ToolHandlingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ToolsListAdapter extends RecyclerView.Adapter<ToolsListAdapter.ViewHolder> {

    private final List<JSONObject> data;
    private int sessionID = -1;
    private ToolHandlingService toolHandlingService;

    public ToolsListAdapter( int sessionID , ToolHandlingService toolHandlingService) {

        this.toolHandlingService = toolHandlingService;
        this.data = toolHandlingService.listTools();
        this.sessionID = sessionID;

        System.out.print("Tools data: "+data);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tool_container, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        JSONObject tool = data.get(position);

        String toolName = null;
        int toolId = -1;

        try {

            toolName = tool.getString("toolName");
            toolId = tool.getInt("toolId");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        holder.txtToolName.setText(toolName);

        holder.itemView.setTag(toolName);

        String finalToolName = toolName;
        int finalToolId = toolId;

        holder.btnDelete.setOnClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            if (toolHandlingService.delete(finalToolId, finalToolName) && toolHandlingService.removeSessionTool(sessionID , finalToolId)) {

                data.remove(pos);
                notifyItemRemoved(pos);

            }

        });

        holder.toolManageButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            JSONObject name = data.get(pos);

            try {

                int toolID = name.getInt("toolId");

                if (toolHandlingService.containsInSessionTool(sessionID, toolID)) {

                    System.out.println("removing tool from session");

                    toolHandlingService.removeSessionTool(sessionID, toolID);

                    holder.toolManageButton.setImageResource(android.R.drawable.ic_input_add);
                    holder.toolManageButton.setColorFilter(Color.parseColor("#4CAF50"));

                } else {

                    System.out.println("adding tool to session");

                    toolHandlingService.addSessionTool(sessionID, toolID);

                    holder.toolManageButton.setImageResource(android.R.drawable.ic_delete);
                    holder.toolManageButton.setColorFilter(Color.parseColor("#FFC107"));
                }


            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            // rebind icon & color
        });


        if (toolHandlingService.containsInSessionTool(sessionID, toolId)) {
            holder.toolManageButton.setImageResource(android.R.drawable.ic_delete);
            holder.toolManageButton.setColorFilter(Color.parseColor("#FFC107"));
        } else {
            holder.toolManageButton.setImageResource(android.R.drawable.ic_input_add);
            holder.toolManageButton.setColorFilter(Color.parseColor("#4CAF50"));
        }





    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtToolName;
        ImageButton btnDelete;

        ImageButton toolManageButton;

        ViewHolder(View itemView) {

            super(itemView);
            txtToolName = itemView.findViewById(R.id.txtToolName);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            toolManageButton = itemView.findViewById(R.id.btnToolManage);


        }





    }
}
