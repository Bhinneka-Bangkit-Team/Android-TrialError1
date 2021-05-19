package com.ocha.prototype_final

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
@SuppressLint("ValidFragment")
class CameraConnectionFragment private constructor(
        private val cameraConnectionCallback:ConnectionCallback
): Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_connection, container, false)
    }



    interface ConnectionCallback {
        fun onPreviewSizeChosen(size: Size, cameraRotation: Int)
    }

    internal class CompareSizeByArea:Comparator<Size>{
        override fun compare(o1: Size, o2: Size): Int {
            return java.lang.Long.signum(
                o1.width.toLong() * o1.height - o2.width* o2.height
            )
        }

    }

    class ErrorDialog : DialogFragment(){
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

            return AlertDialog.Builder(activity)
                .setMessage(arguments?.getString(ARG_MESSAGE))
                .setPositiveButton(
                    "ok"
                ){ dialogInterface, i -> activity?.finish() }
                .create()
        }
        companion object{
            private val ARG_MESSAGE = "message"
            fun newInstance(message: String): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }


//
//    companion object {
//
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            CameraConnectionFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }



}