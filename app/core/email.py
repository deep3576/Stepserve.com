"""
SMTP email utility for StepServe.

All public functions silently log errors rather than raising so that email
failures never break an API response. SMTP credentials are read from
config.ini [email] via app.core.config.settings.
"""
from __future__ import annotations

import logging
import smtplib
from datetime import datetime
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from app.core.config import settings

logger = logging.getLogger("stepserve.email")

# ── internal helpers ──────────────────────────────────────────────────────────

def _smtp_configured() -> bool:
    return bool(settings.smtp_user and settings.smtp_password)


def _send(to: list[str], subject: str, html: str) -> None:
    """Low-level SMTP send.  Raises on failure — callers must catch."""
    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = settings.email_from
    msg["To"] = ", ".join(to)
    msg.attach(MIMEText(html, "html"))

    with smtplib.SMTP(settings.smtp_host, settings.smtp_port, timeout=10) as server:
        server.ehlo()
        server.starttls()
        server.login(settings.smtp_user, settings.smtp_password)
        server.sendmail(settings.smtp_user, to, msg.as_string())


def send_email(to: str | list[str], subject: str, html: str) -> None:
    """Send an HTML email. Logs and swallows exceptions so callers never crash."""
    if not _smtp_configured():
        logger.debug("SMTP not configured — skipping email to %s", to)
        return
    recipients = [to] if isinstance(to, str) else to
    try:
        _send(recipients, subject, html)
        logger.info("Email sent to %s — %s", recipients, subject)
    except Exception as exc:
        logger.error("Failed to send email to %s: %s", recipients, exc)


# ── brand helpers ─────────────────────────────────────────────────────────────

_BRAND_GREEN = "#0a7c5c"
_BRAND_LIGHT = "#f2f9f6"

def _base(title: str, body: str) -> str:
    """Wrap content in a simple, clean HTML email shell."""
    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0" />
<title>{title}</title>
</head>
<body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;color:#333;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:32px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0"
             style="background:#fff;border-radius:8px;overflow:hidden;
                    box-shadow:0 2px 8px rgba(0,0,0,.08);">

        <!-- header -->
        <tr>
          <td style="background:{_BRAND_GREEN};padding:24px 32px;">
            <h1 style="margin:0;color:#fff;font-size:22px;letter-spacing:.5px;">StepServe</h1>
            <p style="margin:4px 0 0;color:#a7f3d0;font-size:13px;">Local services marketplace</p>
          </td>
        </tr>

        <!-- body -->
        <tr>
          <td style="padding:32px;">
            {body}
          </td>
        </tr>

        <!-- footer -->
        <tr>
          <td style="background:{_BRAND_LIGHT};padding:16px 32px;
                     border-top:1px solid #e0e0e0;font-size:12px;color:#757575;
                     text-align:center;">
            &copy; {datetime.utcnow().year} StepServe &mdash; this is an automated message,
            please do not reply directly.
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>"""


def _row(label: str, value: str) -> str:
    return (
        f'<tr>'
        f'<td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;'
        f'           color:#757575;font-size:13px;width:40%;">{label}</td>'
        f'<td style="padding:8px 12px;border-bottom:1px solid #f0f0f0;'
        f'           font-size:13px;font-weight:bold;">{value}</td>'
        f'</tr>'
    )


def _table(*rows: str) -> str:
    inner = "".join(rows)
    return (
        f'<table width="100%" cellpadding="0" cellspacing="0" '
        f'style="border:1px solid #e0e0e0;border-radius:6px;border-collapse:collapse;'
        f'       margin:20px 0;">'
        f'{inner}</table>'
    )


def _badge(text: str, color: str = _BRAND_GREEN) -> str:
    return (
        f'<span style="background:{color};color:#fff;border-radius:4px;'
        f'             padding:3px 10px;font-size:12px;font-weight:bold;">'
        f'{text}</span>'
    )


def _heading(text: str) -> str:
    return f'<h2 style="margin:0 0 8px;font-size:18px;color:{_BRAND_GREEN};">{text}</h2>'


def _subtext(text: str) -> str:
    return f'<p style="color:#757575;font-size:13px;margin:0 0 20px;">{text}</p>'


# ── public template functions ─────────────────────────────────────────────────

def listing_invoice_html(
    *,
    provider_name: str,
    provider_email: str,
    service_title: str,
    service_id: int,
    invoice_id: int,
    amount: float,
    currency: str,
    stripe_pi: str,
    paid_at: datetime | None,
) -> str:
    """Invoice email sent to a provider after their $5 listing fee is paid."""
    paid_str = paid_at.strftime("%B %d, %Y %H:%M UTC") if paid_at else "Just now"
    invoice_no = f"INV-LST-{invoice_id:06d}"

    body = f"""
        {_heading("Listing Payment Invoice")}
        {_subtext(f"Hi {provider_name}, your listing is now <strong>live</strong> on StepServe!")}

        {_table(
            _row("Invoice #", invoice_no),
            _row("Date", paid_str),
            _row("Service", service_title),
            _row("Service ID", f"#{service_id}"),
            _row("Amount", f"{currency} {amount:.2f}"),
            _row("Status", _badge("PAID")),
            _row("Reference", stripe_pi),
        )}

        <p style="font-size:13px;color:#555;margin:20px 0 0;">
          Your listing is now visible to customers. Log in to your
          <strong>StepServe dashboard</strong> to manage bookings and update your profile.
        </p>
        <p style="font-size:12px;color:#9e9e9e;margin:12px 0 0;">
          Billed to: {provider_email}
        </p>
    """
    return _base(f"Invoice {invoice_no} — StepServe", body)


def booking_confirmation_html(
    *,
    customer_name: str,
    customer_email: str,
    service_title: str,
    booking_id: int,
    start_time: datetime,
    end_time: datetime,
    total_price: float,
) -> str:
    """Booking confirmation sent to the customer."""
    ref = f"BK-{booking_id:06d}"
    start_str = start_time.strftime("%B %d, %Y %H:%M UTC")
    end_str = end_time.strftime("%B %d, %Y %H:%M UTC")

    body = f"""
        {_heading("Booking Confirmed!")}
        {_subtext(f"Hi {customer_name}, your booking has been received and is pending provider confirmation.")}

        {_table(
            _row("Booking #", ref),
            _row("Service", service_title),
            _row("Start", start_str),
            _row("End", end_str),
            _row("Total", f"CAD {total_price:.2f}"),
            _row("Status", _badge("PENDING", "#f57c00")),
        )}

        <p style="font-size:13px;color:#555;margin:20px 0 0;">
          You will receive a payment receipt once your booking is confirmed and paid.
          Questions? Contact us at
          <a href="mailto:{settings.email_admin_notify or settings.smtp_user}"
             style="color:{_BRAND_GREEN};">{settings.email_admin_notify or settings.smtp_user}</a>.
        </p>
    """
    return _base(f"Booking {ref} Confirmed — StepServe", body)


def payment_receipt_html(
    *,
    customer_name: str,
    customer_email: str,
    service_title: str,
    booking_id: int,
    payment_id: int,
    amount: float,
    stripe_pi: str,
    paid_at: datetime | None,
) -> str:
    """Payment receipt sent to the customer after a booking is paid."""
    ref = f"BK-{booking_id:06d}"
    receipt_no = f"RCP-{payment_id:06d}"
    paid_str = paid_at.strftime("%B %d, %Y %H:%M UTC") if paid_at else "Just now"

    body = f"""
        {_heading("Payment Receipt")}
        {_subtext(f"Hi {customer_name}, your payment has been received. Thank you!")}

        {_table(
            _row("Receipt #", receipt_no),
            _row("Booking #", ref),
            _row("Date", paid_str),
            _row("Service", service_title),
            _row("Amount Paid", f"CAD {amount:.2f}"),
            _row("Status", _badge("PAID")),
            _row("Reference", stripe_pi),
        )}

        <p style="font-size:13px;color:#555;margin:20px 0 0;">
          Your booking is now <strong>confirmed</strong>. Please arrive a few minutes early
          and bring any materials requested by your instructor.
        </p>
        <p style="font-size:12px;color:#9e9e9e;margin:12px 0 0;">
          Receipt sent to: {customer_email}
        </p>
    """
    return _base(f"Payment Receipt {receipt_no} — StepServe", body)


def new_booking_admin_html(
    *,
    customer_email: str,
    service_title: str,
    booking_id: int,
    start_time: datetime,
    end_time: datetime,
    total_price: float,
) -> str:
    """Admin notification for a new booking."""
    ref = f"BK-{booking_id:06d}"
    start_str = start_time.strftime("%B %d, %Y %H:%M UTC")
    end_str = end_time.strftime("%B %d, %Y %H:%M UTC")

    body = f"""
        {_heading("New Booking Received")}
        {_subtext("A new booking has been placed on StepServe.")}

        {_table(
            _row("Booking #", ref),
            _row("Customer", customer_email),
            _row("Service", service_title),
            _row("Start", start_str),
            _row("End", end_str),
            _row("Total", f"CAD {total_price:.2f}"),
        )}
    """
    return _base(f"New Booking {ref} — Admin Alert", body)


def password_reset_html(*, email: str, code: str, expires_minutes: int = 15) -> str:
    """Password reset code email sent to the user."""
    body = f"""
        {_heading("Reset Your Password")}
        {_subtext("We received a request to reset the password for your StepServe account.")}

        <div style="text-align:center;margin:32px 0;">
          <p style="margin:0 0 8px;font-size:14px;color:#555;">Your reset code is:</p>
          <div style="display:inline-block;background:{_BRAND_GREEN};color:#fff;
                      font-size:36px;font-weight:bold;letter-spacing:10px;
                      padding:16px 32px;border-radius:10px;">
            {code}
          </div>
          <p style="margin:16px 0 0;font-size:13px;color:#9e9e9e;">
            This code expires in <strong>{expires_minutes} minutes</strong>.
          </p>
        </div>

        <p style="font-size:13px;color:#555;margin:0 0 8px;">
          Enter this code in the StepServe app along with your new password to complete the reset.
        </p>
        <p style="font-size:12px;color:#9e9e9e;margin:0;">
          If you did not request a password reset, you can safely ignore this email.
          Your password will not be changed.
        </p>
        <p style="font-size:12px;color:#9e9e9e;margin:12px 0 0;">
          Account: {email}
        </p>
    """
    return _base("Password Reset Code — StepServe", body)
