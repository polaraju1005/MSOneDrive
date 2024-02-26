package com.example.msonedrive

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File

class FileListAdapter(private val context: FilePickerActivity, private val filesList: List<File>) :
    ArrayAdapter<File>(context, 0, filesList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layout = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_file, parent, false)

        val tvFileName: TextView = layout.findViewById(R.id.tvFileName)
        val ivDelete: ImageView = layout.findViewById(R.id.ivDelete)

        val file = filesList[position]
        tvFileName.text = file.name


        ivDelete.setOnClickListener {
            // Retrieve OneDrive file ID using the local file name
//            val oneDriveFileId = getOneDriveFileId(context, file.name)
            val oneDriveFileId = (context as? FilePickerActivity)?.getOneDriveFileId(file.name)
            if (oneDriveFileId != null) {
                (context as? FilePickerActivity)?.deleteFileFromOneDrive(oneDriveFileId)
            } else {
                // Handle case where there's no mapping found, if necessary
            }

            // Continue with local file deletion and ListView update
            if (file.delete()) {
                (filesList as MutableList<File>).removeAt(position)
                notifyDataSetChanged() // Refresh the ListView
            } else {
                // Optionally handle failure to delete file locally
            }
        }

        return layout
    }
}
