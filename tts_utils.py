from gtts import gTTS

# TTS로 응답 텍스트를 음성으로 변환
def text_to_speech(text, output_file="output_audio.mp3"):
    tts = gTTS(text=text, lang='ko')
    tts.save(output_file)
    return output_file