import firebase_admin
from firebase_admin import credentials, db, storage


def initialize_firebase(service_account_path, database_url, storage_bucket):
    """
    Firebase Realtime Database와 Storage를 초기화합니다.
    """
    if not firebase_admin._apps:  # Firebase 초기화 상태 체크
        cred = credentials.Certificate(service_account_path)
        firebase_admin.initialize_app(cred, {
            "databaseURL": database_url,
            "storageBucket": storage_bucket
        })


# Realtime Database에서 데이터 가져오기
def get_realtime_data(ref_path):
    try:
        ref = db.reference(ref_path)
        data = ref.get()  # 데이터 읽기
        return data
    except Exception as e:
        print(f"Realtime Database에서 데이터를 가져오는 중 오류 발생: {e}")
        return None


# Realtime Database에서 데이터 변경 이벤트 수신
def add_realtime_listener(ref_path, callback):
    try:
        ref = db.reference(ref_path)
        ref.listen(callback)  # 데이터 변경 리스너 추가
    except Exception as e:
        print(f"Realtime Database 리스너 설정 중 오류 발생: {e}")


# Firebase Storage 버킷 가져오기
def get_storage_bucket():
    return storage.bucket()


# Firebase에 음성 파일 업로드 및 URL 반환
def upload_audio(bucket, file_path, file_name="temperature_audio.mp3"):
    try:
        blob = bucket.blob(file_name)
        print(f"Uploading file: {file_path} to bucket: {bucket.name}")
        blob.upload_from_filename(file_path)

        # 명시적으로 퍼블릭 읽기 권한 설정
        blob.make_public()  # 퍼블릭 URL 생성 가능
        

        # 생성된 퍼블릭 URL 반환
        public_url = blob.public_url
        print(f"Uploaded file is publicly accessible at: {public_url}")
        return public_url
    except Exception as e:
        print(f"파일 업로드 중 오류 발생: {e}")
        return None
