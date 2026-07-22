"""
تحويل نموذج MONAI / PyTorch إلى صيغة ONNX تعمل على أندرويد بدون إنترنت.

مثال: تحويل DenseNet121 من MONAI (مدرب مسبقًا) إلى model.onnx
جاهز للوضع في: app/src/main/assets/models/densenet121_chest_xray/model.onnx

الاستخدام:
    pip install monai torch onnx onnxruntime
    python convert_to_onnx.py --checkpoint path/to/model.pt --output model.onnx

ملاحظات مهمة:
- الشكل (shape) للمُدخل هنا (1, 3, 224, 224) لازم يطابق input_width/input_height
  في config.json الخاص بنفس النموذج.
- opset_version=17 متوافق مع onnxruntime-android 1.18.0 المستخدم في build.gradle.kts.
- بعد التحويل، تأكد إن حجم الملف الناتج معقول لتطبيق موبايل (راجع file_size_mb
  في config.json وحدّثه ليطابق الحجم الفعلي).
"""

import argparse

import torch
from monai.networks.nets import DenseNet121


def convert(checkpoint_path: str, output_path: str, num_classes: int = 4):
    # عدّل هذا الجزء حسب النموذج الفعلي اللي هتحمّله من MONAI Model Zoo
    model = DenseNet121(
        spatial_dims=2,
        in_channels=3,
        out_channels=num_classes,
    )

    state_dict = torch.load(checkpoint_path, map_location="cpu")
    # بعض checkpoints بتكون ملفوفة في مفتاح "state_dict" أو "model"
    if "state_dict" in state_dict:
        state_dict = state_dict["state_dict"]
    elif "model" in state_dict:
        state_dict = state_dict["model"]

    model.load_state_dict(state_dict, strict=False)
    model.eval()

    dummy_input = torch.randn(1, 3, 224, 224)

    torch.onnx.export(
        model,
        dummy_input,
        output_path,
        export_params=True,
        opset_version=17,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes=None,  # شكل ثابت أبسط وأسرع للاستدلال على الموبايل
    )

    print(f"تم التصدير بنجاح إلى: {output_path}")
    print("الخطوة التالية: انسخ الملف إلى")
    print("  app/src/main/assets/models/<model_id>/model.onnx")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkpoint", required=True, help="مسار ملف .pt أو .pth")
    parser.add_argument("--output", default="model.onnx", help="مسار ملف الإخراج")
    parser.add_argument("--num-classes", type=int, default=4)
    args = parser.parse_args()

    convert(args.checkpoint, args.output, args.num_classes)
