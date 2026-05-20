from django.urls import path, include
from rest_framework.routers import DefaultRouter
from rest_framework_simplejwt.views import (
    TokenObtainPairView,
    TokenRefreshView,
)
from .views import PatientViewSet, CareItemViewSet, AssistantViewSet, RegisterView

app_name = 'api'

router = DefaultRouter()
router.register(r'patients', PatientViewSet, basename='patient')
router.register(r'care-items', CareItemViewSet, basename='careitem')
router.register(r'assistant', AssistantViewSet, basename='assistant')

urlpatterns = [
    # Authentication endpoints
    path('auth/token/', TokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('auth/token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('auth/register/', RegisterView.as_view(), name='register'),
    
    # Router endpoints
    path('', include(router.urls)),
]
