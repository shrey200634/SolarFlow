import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AuthLayout } from '../layouts/AuthLayout'
import { CheckboxField, TextField } from '../components/forms/fields'
import { Button } from '../components/common/Button'
import { Alert } from '../components/feedback/Alert'
import { useAuth } from '../features/auth/auth-context'
import { parseError } from '../api/client'

// Mirrors backend RegisterRequestDTO
const schema = z.object({
  fullName: z.string().trim().min(1, 'Enter your full name').max(120, 'Use 120 characters or fewer'),
  email: z.string().min(1, 'Enter your email').email('Enter a valid email').max(255),
  phone: z.string().regex(/^(\d{10})?$/, 'Enter a 10-digit mobile number'),
  password: z.string().min(8, 'Use at least 8 characters').max(72, 'Use 72 characters or fewer'),
  consent: z.boolean().refine((v) => v, 'You need to agree to continue'),
})
type FormValues = z.infer<typeof schema>

export default function RegisterPage() {
  const { register: signUp } = useAuth()
  const [error, setError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError: setFieldError,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', phone: '', password: '', consent: false },
  })

  const onSubmit = async (v: FormValues) => {
    setError(null)
    try {
      await signUp({
        fullName: v.fullName,
        email: v.email,
        phone: v.phone || undefined,
        password: v.password,
        consent: true,
      })
    } catch (e) {
      const p = parseError(e)
      if (p.status === 409) {
        setFieldError('email', { message: 'An account with this email already exists.' })
        return
      }
      const known = ['fullName', 'email', 'phone', 'password', 'consent'] as const
      let mapped = false
      for (const key of known) {
        if (p.fieldErrors[key]) {
          setFieldError(key, { message: p.fieldErrors[key] })
          mapped = true
        }
      }
      if (!mapped) setError(p.message)
    }
  }

  return (
    <AuthLayout title="Create your account">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label="Full name" autoComplete="name" error={errors.fullName?.message} {...register('fullName')} />
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <TextField label="Mobile number" inputMode="numeric" maxLength={10} autoComplete="tel-national" hint="Optional. 10 digits." error={errors.phone?.message} {...register('phone')} />
        <TextField label="Password" type="password" autoComplete="new-password" hint="At least 8 characters." error={errors.password?.message} {...register('password')} />
        <CheckboxField
          label="I agree that SolarFlow may store my details to process my solar enquiry."
          error={errors.consent?.message}
          {...register('consent')}
        />
        <Button type="submit" className="w-full" loading={isSubmitting}>Create account</Button>
      </form>
      <p className="mt-5 text-center text-sm text-muted">
        Already registered? <Link to="/login" className="font-medium text-brand hover:underline">Sign in</Link>
      </p>
    </AuthLayout>
  )
}