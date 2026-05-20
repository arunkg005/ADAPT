from rest_framework import viewsets, status
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated, AllowAny
from rest_framework.views import APIView

from django.contrib.auth.models import User
from rest_framework_simplejwt.tokens import RefreshToken

from patients.models import Patient
from tasks.models import CareItem
from assistant.models import ChatSession, ChatMessage
from assistant.services import generate_assistant_reply, apply_action_proposal

from .serializers import PatientSerializer, CareItemSerializer

class PatientViewSet(viewsets.ModelViewSet):
    serializer_class = PatientSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Patient.objects.filter(user=self.request.user)

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)

class CareItemViewSet(viewsets.ModelViewSet):
    queryset = CareItem.objects.all()
    serializer_class = CareItemSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        qs = CareItem.objects.filter(patient__user=self.request.user)
        patient_id = self.request.query_params.get('patient_id')
        if patient_id:
            return qs.filter(patient_id=patient_id)
        return qs

class AssistantViewSet(viewsets.ViewSet):
    permission_classes = [IsAuthenticated]

    @action(detail=False, methods=['post'])
    def ask(self, request):
        patient_id = request.data.get('patient_id')
        user_text = request.data.get('text')
        session_id = request.data.get('session_id')

        if not user_text:
            return Response({'error': 'No text provided'}, status=status.HTTP_400_BAD_REQUEST)

        patient = None
        if patient_id:
            patient = Patient.objects.filter(id=patient_id, user=request.user).first()

        # Get or create session
        if session_id:
            session = ChatSession.objects.filter(id=session_id).first()
        else:
            session = ChatSession.objects.create(patient=patient, source='mobile')

        if not session:
             session = ChatSession.objects.create(patient=patient, source='mobile')

        # Add user message
        ChatMessage.objects.create(session=session, role='user', content=user_text)

        # Generate reply
        # Note: In a real app, you might want to fetch previous summaries from DB
        previous_summaries = [] 
        reply, action_proposal = generate_assistant_reply(session, patient, user_text, previous_summaries)

        # Add assistant message
        ChatMessage.objects.create(session=session, role='assistant', content=reply)

        response_data = {
            'reply': reply,
            'session_id': session.id,
            'action_proposal': action_proposal
        }

        return Response(response_data)

    @action(detail=False, methods=['post'])
    def confirm_action(self, request):
        patient_id = request.data.get('patient_id')
        action_payload = request.data.get('action_proposal')

        if not patient_id or not action_payload:
            return Response({'error': 'Patient ID and action proposal required'}, status=status.HTTP_400_BAD_REQUEST)

        patient = Patient.objects.filter(id=patient_id, user=request.user).first()
        if not patient:
            return Response({'error': 'Patient not found'}, status=status.HTTP_404_NOT_FOUND)

        op = apply_action_proposal(patient, action_payload)
        return Response({'status': 'success', 'operation': op})


class RegisterView(APIView):
    """Mobile registration endpoint — creates user + returns JWT tokens."""
    permission_classes = [AllowAny]

    def post(self, request):
        username = request.data.get('username', '').strip()
        password = request.data.get('password', '').strip()

        if not username or not password:
            return Response(
                {'error': 'Username and password are required.'},
                status=status.HTTP_400_BAD_REQUEST,
            )

        if User.objects.filter(username=username).exists():
            return Response(
                {'error': 'Username already taken.'},
                status=status.HTTP_409_CONFLICT,
            )

        user = User.objects.create_user(username=username, password=password)
        refresh = RefreshToken.for_user(user)

        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
        }, status=status.HTTP_201_CREATED)
