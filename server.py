import os
import torchaudio

# =================================================================
# 🚑 CRITICAL HOTFIX FOR MACOS / TORCHAUDIO 2.0+
# =================================================================
# SpeechBrain is looking for 'list_audio_backends', but Torchaudio deleted it.
# We manually add it back so SpeechBrain doesn't crash.
if not hasattr(torchaudio, "list_audio_backends"):
    def _list_audio_backends():
        return ["soundfile"]
    torchaudio.list_audio_backends = _list_audio_backends

# Force Torchaudio to use 'soundfile' (stable) instead of 'torchcodec' (broken)
try:
    torchaudio.set_audio_backend("soundfile")
except:
    pass
# =================================================================

from flask import Flask, request, jsonify
from speechbrain.inference.speaker import SpeakerRecognition
import speech_recognition as sr
from pydub import AudioSegment

app = Flask(__name__)

# --- CONFIGURATION ---
UPLOAD_FOLDER = 'uploads'
ENROLLED_FILE = 'enrolled_user.wav'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# 1. Initialize the "Gatekeeper" (Speaker Verification Model)
print("Loading AI Models... (This may take a minute)")
verification_model = SpeakerRecognition.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb",
    savedir="pretrained_models/spkrec-ecapa-voxceleb"
)
print("Models Loaded!")

# 2. Initialize the "Transcriber" (Google Speech API)
recognizer = sr.Recognizer()

def convert_to_wav(filepath):
    """Ensures audio is in the correct WAV format for processing (16kHz Mono)"""
    try:
        audio = AudioSegment.from_file(filepath)
        audio = audio.set_frame_rate(16000).set_channels(1)
        new_path = filepath.rsplit('.', 1)[0] + "_converted.wav"
        audio.export(new_path, format="wav")
        return new_path
    except Exception as e:
        print(f"Conversion Error: {e}")
        return filepath

@app.route('/enroll', methods=['POST'])
def enroll():
    """Endpoint to save the owner's voice"""
    if 'audio' not in request.files:
        return jsonify({"error": "No audio file"}), 400
    
    file = request.files['audio']
    save_path = os.path.join(UPLOAD_FOLDER, "temp_enroll.m4a")
    file.save(save_path)
    
    # Convert and save as the "Master" voice
    final_path = convert_to_wav(save_path)
    
    if os.path.exists(ENROLLED_FILE):
        os.remove(ENROLLED_FILE)
    os.rename(final_path, ENROLLED_FILE)
    
    print("✅ New User Enrolled")
    return jsonify({"message": "Enrollment Successful! Voice registered."})

@app.route('/process', methods=['POST'])
def process():
    """Endpoint to verify speaker and transcribe text"""
    if 'audio' not in request.files:
        return jsonify({"error": "No audio file"}), 400
    
    # 1. Save the incoming query
    file = request.files['audio']
    query_path = os.path.join(UPLOAD_FOLDER, "query.m4a")
    file.save(query_path)
    
    # Convert to WAV for processing
    query_wav_path = convert_to_wav(query_path)
    
    # 2. CHECK: Is Enrolled?
    if not os.path.exists(ENROLLED_FILE):
        return jsonify({"authorized": False, "message": "No user enrolled yet!"}), 400

    # 3. VERIFY: Is this the owner?
    # verify_files returns: score, prediction
    score, prediction = verification_model.verify_files(ENROLLED_FILE, query_wav_path)
    
    # Threshold: Lowered slightly for phone mic usage
    is_authorized = score.item() > 0.20 
    
    print(f"🔍 Speaker Score: {score.item():.4f} | Authorized: {is_authorized}")

    if not is_authorized:
        return jsonify({
            "authorized": False,
            "message": "Voice not recognized (Ignored)"
        })

    # 4. TRANSCRIBE: Convert to text (if authorized)
    transcript = ""
    try:
        with sr.AudioFile(query_wav_path) as source:
            audio_data = recognizer.record(source)
            transcript = recognizer.recognize_google(audio_data)
            print(f"🗣️ Transcript: {transcript}")
    except sr.UnknownValueError:
        transcript = ""
        print("🗣️ Transcript: [Unintelligible]")
    except Exception as e:
        transcript = f"Error: {e}"
        print(f"❌ Error: {e}")

    return jsonify({
        "authorized": True,
        "text": transcript
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001)
